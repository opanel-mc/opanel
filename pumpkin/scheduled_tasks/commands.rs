use std::{future::Future, time::Duration};

use tokio_util::sync::CancellationToken;

#[derive(Clone, Debug, PartialEq)]
pub enum Command {
    Server(String),
    Loop(u32, Vec<Command>),
    Sleep(Option<String>),
    Restart,
}

#[derive(Debug, PartialEq)]
pub enum Action {
    Server(String),
    Restart,
}

pub fn parse(lines: &[String]) -> Result<Vec<Command>, String> {
    let mut commands = Vec::new();
    let mut current_loop: Option<(u32, Vec<String>)> = None;
    for line in lines {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        if let Some((_, lines)) = &mut current_loop {
            if line.starts_with("@end") {
                let (times, lines) = current_loop.take().unwrap();
                commands.push(Command::Loop(times, parse(&lines)?));
            } else {
                lines.push(line.to_string());
            }
            continue;
        }
        let parts: Vec<_> = line.split(' ').collect();
        if line.starts_with("@loop") {
            if parts.len() != 2 {
                return Err("Illegal loop syntax.".into());
            }
            let times = parts[1]
                .parse::<i32>()
                .map_err(|_| "Illegal loop syntax.")?;
            if times <= 0 {
                return Err("Loop times must be greater than 0.".into());
            }
            current_loop = Some((times as u32, Vec::new()));
        } else if line.starts_with("@goto") || line.starts_with("@sign") {
            if parts.len() != 2 {
                return Err("Illegal goto/sign syntax.".into());
            }
            // Java parses these nodes but its executor does not implement jumps.
        } else if let Some(operation) = parts[0].strip_prefix('@') {
            match operation {
                "sleep" => {
                    commands.push(Command::Sleep(parts.get(1).map(|value| value.to_string())))
                }
                "restart" => commands.push(Command::Restart),
                _ => return Err(format!("Unknown builtin operation '{operation}'.")),
            }
        } else {
            commands.push(Command::Server(line.to_string()));
        }
    }
    // Preserve Java's behavior: an unterminated loop and its body are discarded.
    Ok(commands)
}

pub async fn execute<F, Fut>(
    commands: &[Command],
    variables: &[(&str, String)],
    cancelled: &CancellationToken,
    send: &mut F,
) -> Result<(), String>
where
    F: FnMut(Action) -> Fut + Send,
    Fut: Future<Output = Result<(), String>> + Send,
{
    for command in commands {
        if cancelled.is_cancelled() {
            return Ok(());
        }
        let operation = async {
            match command {
                Command::Server(command) => send(Action::Server(inject(command, variables))).await,
                Command::Restart => send(Action::Restart).await,
                Command::Sleep(milliseconds) => {
                    let milliseconds = milliseconds
                        .as_deref()
                        .ok_or("Missing sleep millisecond parameter.")?
                        .parse::<u64>()
                        .ok()
                        .filter(|value| *value > 0)
                        .ok_or("Invalid sleep millisecond parameter.")?;
                    tokio::time::sleep(Duration::from_millis(milliseconds)).await;
                    Ok(())
                }
                Command::Loop(times, children) => {
                    for _ in 0..*times {
                        if cancelled.is_cancelled() {
                            break;
                        }
                        Box::pin(execute(children, variables, cancelled, send)).await?;
                        tokio::task::yield_now().await;
                    }
                    Ok(())
                }
            }
        };
        tokio::select! {
            biased;
            _ = cancelled.cancelled() => return Ok(()),
            result = operation => result?,
        }
    }
    Ok(())
}

fn inject(command: &str, variables: &[(&str, String)]) -> String {
    let mut result = command.to_string();
    for (name, value) in variables {
        result = result.replace(&format!("@{{{name}}}"), value);
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;
    fn lines(values: &[&str]) -> Vec<String> {
        values.iter().map(|value| value.to_string()).collect()
    }

    #[tokio::test]
    async fn executes_loops_variables_and_restart_in_order() {
        let program = parse(&lines(&[
            "# comment",
            "@loop 2",
            "say @{motd}",
            "@end",
            "@sleep 1",
            "@restart",
        ]))
        .unwrap();
        let mut actions = Vec::new();
        execute(
            &program,
            &[("motd", "hello $world\\".into())],
            &CancellationToken::new(),
            &mut |action| {
                actions.push(action);
                std::future::ready(Ok(()))
            },
        )
        .await
        .unwrap();
        assert_eq!(
            actions,
            [
                Action::Server("say hello $world\\".into()),
                Action::Server("say hello $world\\".into()),
                Action::Restart
            ]
        );
    }

    #[test]
    fn preserves_java_silent_commands_and_rejects_invalid_builtins() {
        assert_eq!(
            parse(&lines(&[
                "say before",
                "@goto end",
                "@sign end",
                "@loop 2",
                "say ignored"
            ]))
            .unwrap(),
            [Command::Server("say before".into())]
        );
        for input in [
            vec!["@loop 0"],
            vec!["@loop invalid"],
            vec!["@unknown"],
            vec!["@goto"],
        ] {
            assert!(parse(&lines(&input)).is_err());
        }
        assert_eq!(parse(&lines(&["@sleep"])).unwrap(), [Command::Sleep(None)]);
    }

    #[tokio::test]
    async fn cancellation_interrupts_sleep_without_running_later_commands() {
        let cancelled = CancellationToken::new();
        let token = cancelled.clone();
        let (sender, mut receiver) = tokio::sync::mpsc::unbounded_channel();
        let worker = tokio::spawn(async move {
            execute(
                &parse(&lines(&["say first", "@sleep 60000", "say never"])).unwrap(),
                &[],
                &token,
                &mut |action| {
                    sender.send(action).unwrap();
                    std::future::ready(Ok(()))
                },
            )
            .await
            .unwrap();
        });
        assert_eq!(
            receiver.recv().await,
            Some(Action::Server("say first".into()))
        );
        cancelled.cancel();
        tokio::time::timeout(Duration::from_secs(1), worker)
            .await
            .unwrap()
            .unwrap();
        assert_eq!(receiver.recv().await, None);
    }
}
