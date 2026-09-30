use std::{process::Command, sync::Arc};

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
    let mut event = ServerCommandEvent::new(command);
    server.plugin_manager.fire(server, &mut event).await;
    if !event.cancelled {
        server
            .command_dispatcher
            .load()
            .handle_command(&CommandSender::Console.into_source(server), &event.command);
    }
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
    fn restart_command_enforces_a_positive_delay_and_keeps_the_launch_command() {
        let restart = restart_command("pumpkin --world 'main'", 0);
        let joined = restart.args.join(" ");

        assert!(joined.contains("10"));
        assert!(joined.contains("pumpkin"));
        assert!(joined.contains("main"));
    }
}
