use std::{
    process::Command,
    sync::{Arc, Mutex},
};

use pumpkin::{
    command::CommandSender, plugin::api::events::server::server_command::ServerCommandEvent,
    server::Server,
};

use thiserror::Error;

use crate::{
    opanel::OPanel,
    storage::{StorageError, TextFile},
};

pub(crate) const LAUNCH_COMMAND_FILE: TextFile = TextFile::new("launch-command.txt", "");

#[derive(Debug, Error)]
pub(crate) enum RestartError {
    #[error("Launch command is not set.")]
    MissingLaunchCommand,
    #[error(transparent)]
    Storage(#[from] StorageError),
    #[error(transparent)]
    Io(#[from] std::io::Error),
}

#[derive(Debug)]
struct RestartCommand {
    program: &'static str,
    args: Vec<String>,
}

pub(crate) async fn send_command(server: &Arc<Server>, command: String) {
    dispatch_command(server, command, CommandSender::Console).await;
}

pub(crate) struct CommandOutput(Arc<Mutex<Vec<String>>>);

impl CommandOutput {
    pub(crate) fn drain(&self) -> (Vec<String>, bool) {
        // Async commands retain a cloned sender until their final feedback has been written.
        let finished = Arc::strong_count(&self.0) == 1;
        let lines = std::mem::take(
            &mut *self
                .0
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner),
        );
        // Console feedback uses stdout and is absent from Pumpkin's log file.
        for line in &lines {
            println!("{line}");
        }
        (lines, finished)
    }
}

pub(crate) async fn send_command_with_output(
    server: &Arc<Server>,
    command: String,
) -> CommandOutput {
    let output = CommandOutput(Arc::new(Mutex::new(Vec::new())));
    dispatch_command(server, command, CommandSender::Rcon(Arc::clone(&output.0))).await;
    output
}

async fn dispatch_command(server: &Arc<Server>, command: String, output: CommandSender) {
    let mut event = ServerCommandEvent::new(command);
    server.plugin_manager.fire(server, &mut event).await;
    if !event.cancelled {
        server.command_dispatcher.load().handle_command(
            &CommandSender::Console
                .into_source(server)
                .with_output(output),
            &event.command,
        );
    }
}

pub(crate) fn get_commands(server: &Server) -> Vec<String> {
    server
        .command_dispatcher
        .load()
        .get_all_commands()
        .keys()
        .map(ToString::to_string)
        .collect()
}

pub(crate) fn get_command_suggestions(
    server: &Arc<Server>,
    command: &str,
    arg_index: usize,
) -> Vec<String> {
    if arg_index == 1 {
        return get_commands(server);
    }
    let Some(prefix) = completion_prefix(command, arg_index) else {
        return Vec::new();
    };
    server
        .command_dispatcher
        .load()
        .suggest(prefix, &CommandSender::Console.into_source(server))
        .into_iter()
        .map(|suggestion| suggestion.suggestion)
        .collect()
}

fn completion_prefix(command: &str, arg_index: usize) -> Option<&str> {
    let command = command.strip_prefix('/').unwrap_or(command);
    let (space, _) = command.match_indices(' ').nth(arg_index.checked_sub(2)?)?;
    // The frontend filters candidates itself and may be editing an earlier argument.
    Some(&command[..space + 1])
}

pub(crate) async fn restart(opanel: &OPanel) -> Result<(), RestartError> {
    let launch_command = opanel.storage().read_text(&LAUNCH_COMMAND_FILE).await?;
    if launch_command.is_empty() {
        return Err(RestartError::MissingLaunchCommand);
    }
    let restart = restart_command(&launch_command, opanel.config().server_restart_delay);
    Command::new(restart.program)
        .args(restart.args)
        .current_dir(std::env::current_dir()?)
        .spawn()?;
    pumpkin::stop_server();
    Ok(())
}
fn restart_command(launch_command: &str, delay_seconds: u64) -> RestartCommand {
    let delay_seconds = if delay_seconds == 0 {
        10
    } else {
        delay_seconds
    };
    if cfg!(windows) {
        RestartCommand {
            program: "cmd.exe",
            args: vec![
                "/C".to_string(),
                "start".to_string(),
                String::new(),
                "cmd.exe".to_string(),
                "/C".to_string(),
                format!("timeout /T {delay_seconds} /NOBREAK > NUL && {launch_command}"),
            ],
        }
    } else {
        let command = launch_command.replace('\'', "'\\''");
        RestartCommand {
            program: "sh",
            args: vec![
                "-c".to_string(),
                format!("nohup sh -c 'sleep {delay_seconds} && {command}' >/dev/null 2>&1 &"),
            ],
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn command_output_keeps_collecting_until_async_senders_are_released() {
        let output = CommandOutput(Arc::new(Mutex::new(Vec::new())));
        let sender = CommandSender::Rcon(Arc::clone(&output.0));
        sender.send_message(pumpkin_util::text::TextComponent::text("started"));
        let (lines, finished) = output.drain();
        assert_eq!(lines.len(), 1);
        assert!(lines[0].contains("started"));
        assert!(!finished);
        assert!(output.drain().0.is_empty());

        sender.send_message(pumpkin_util::text::TextComponent::text("finished later"));
        drop(sender);
        let (lines, finished) = output.drain();
        assert_eq!(lines.len(), 1);
        assert!(lines[0].contains("finished later"));
        assert!(finished);
        assert!(output.drain().0.is_empty());
    }

    #[test]
    fn completion_uses_the_requested_argument_and_preserves_unicode_and_spaces() {
        for (command, index, expected) in [
            ("time set day", 2, Some("time ")),
            ("time set day", 3, Some("time set ")),
            ("/give 玩家 minecraft:stone", 3, Some("give 玩家 ")),
            ("time  set", 3, Some("time  ")),
            ("time ", 2, Some("time ")),
            ("time", 2, None),
            ("time set", 4, None),
            ("time set", 0, None),
        ] {
            assert_eq!(completion_prefix(command, index), expected);
        }
    }

    #[test]
    fn restart_command_enforces_a_positive_delay_and_keeps_the_launch_command() {
        let restart = restart_command("pumpkin --world 'main'", 0);
        let joined = restart.args.join(" ");

        assert!(joined.contains("10"));
        assert!(joined.contains("pumpkin"));
        assert!(joined.contains("main"));
    }
}
