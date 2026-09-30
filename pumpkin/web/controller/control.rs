use std::{process::Command, sync::Arc};

use axum::{
    body::Bytes,
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64_STANDARD};
use pumpkin::data::datapack::DatapackManager;
use serde::{Deserialize, Serialize};
use tracing::error;

use crate::{
    opanel::OPanel,
    save::{Save, SaveError},
    storage::TextFile,
    utils::pumpkin_config,
    web::response::{ApiError, ApiResponse},
};

const LAUNCH_COMMAND_FILE: TextFile = TextFile::new("launch-command.txt", "");

#[derive(Debug, Serialize)]
struct PropertiesPayload {
    properties: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct LaunchCommandPayload {
    launch_command: String,
}

#[derive(Debug, Serialize)]
pub(super) struct EmptyPayload {}

#[derive(Debug, Deserialize)]
pub(super) struct SwitchSaveQuery {
    save: Option<String>,
}

#[derive(Debug)]
struct RestartCommand {
    program: &'static str,
    args: Vec<String>,
}

pub(super) async fn get_server_properties(State(_opanel): State<Arc<OPanel>>) -> Response {
    match pumpkin_config::read().await {
        Ok(properties) => ApiResponse::ok(PropertiesPayload {
            properties: BASE64_STANDARD.encode(properties),
        })
        .into_response(),
        Err(error) => {
            error!(%error, path = pumpkin_config::PUMPKIN_CONFIG_PATH, "failed to read Pumpkin configuration");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        }
    }
}

pub(super) async fn set_server_properties(
    State(_opanel): State<Arc<OPanel>>,
    body: Bytes,
) -> Response {
    if body.is_empty() {
        return ApiError::new(
            StatusCode::BAD_REQUEST,
            "Pumpkin configuration content is missing.",
        )
        .into_response();
    }

    let decoded = match BASE64_STANDARD.decode(&body) {
        Ok(decoded) => decoded,
        Err(error) => {
            error!(%error, "invalid Base64 encoding in Pumpkin configuration");
            return ApiError::new(
                StatusCode::BAD_REQUEST,
                "Invalid Base64 encoding in Pumpkin configuration.",
            )
            .into_response();
        }
    };
    let configuration = match String::from_utf8(decoded) {
        Ok(configuration) => configuration,
        Err(error) => {
            error!(%error, "Pumpkin configuration is not UTF-8");
            return ApiError::new(
                StatusCode::BAD_REQUEST,
                "Pumpkin configuration must be UTF-8.",
            )
            .into_response();
        }
    };
    if let Err(error) = pumpkin_config::parse(&configuration) {
        error!(%error, "invalid Pumpkin configuration");
        return ApiError::new(StatusCode::BAD_REQUEST, "Invalid Pumpkin configuration.")
            .into_response();
    }

    match pumpkin_config::write(configuration).await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => {
            error!(%error, path = pumpkin_config::PUMPKIN_CONFIG_PATH, "failed to write Pumpkin configuration");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        }
    }
}

pub(super) async fn get_code_of_conducts(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_code_of_conduct()
}

pub(super) async fn change_code_of_conduct(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_code_of_conduct()
}

pub(super) async fn remove_code_of_conduct(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_code_of_conduct()
}

pub(super) async fn stop_server(State(_opanel): State<Arc<OPanel>>) -> ApiResponse<EmptyPayload> {
    pumpkin::stop_server();
    ApiResponse::ok(EmptyPayload {})
}

pub(super) async fn reload_server(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = &opanel.context().server;
    match DatapackManager::reload(server) {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => {
            error!(%error, "failed to reload Pumpkin datapacks");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error).into_response()
        }
    }
}

pub(super) async fn restart_server(State(opanel): State<Arc<OPanel>>) -> Response {
    let launch_command = match opanel.storage().read_text(&LAUNCH_COMMAND_FILE).await {
        Ok(command) => command,
        Err(error) => {
            error!(%error, "failed to read launch command");
            return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
                .into_response();
        }
    };
    if launch_command.is_empty() {
        return ApiError::new(StatusCode::NOT_ACCEPTABLE, "Launch command is not set.")
            .into_response();
    }

    let restart = restart_command(&launch_command, opanel.config().server_restart_delay);
    let current_directory = match std::env::current_dir() {
        Ok(directory) => directory,
        Err(error) => {
            error!(%error, "failed to resolve the server working directory");
            return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
                .into_response();
        }
    };
    if let Err(error) = Command::new(restart.program)
        .args(restart.args)
        .current_dir(current_directory)
        .spawn()
    {
        error!(%error, "failed to schedule Pumpkin restart");
        return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response();
    }

    pumpkin::stop_server();
    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn switch_save(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<SwitchSaveQuery>,
) -> Response {
    let Some(save_name) = query.save else {
        return ApiError::new(StatusCode::BAD_REQUEST, "Save name is missing.").into_response();
    };
    let save = match Save::open(Arc::clone(&opanel.context().server), &save_name).await {
        Ok(save) => save,
        Err(SaveError::InvalidName) => {
            return ApiError::new(StatusCode::BAD_REQUEST, "Illegal save name.").into_response();
        }
        Err(SaveError::NotFound) => {
            return ApiError::new(StatusCode::NOT_FOUND, "Cannot find the save.").into_response();
        }
        Err(error) => {
            error!(%error, save = save_name, "failed to open Pumpkin save");
            return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
                .into_response();
        }
    };
    match save.set_current().await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => {
            error!(%error, save = save_name, "failed to switch Pumpkin save");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        }
    }
}

pub(super) async fn get_paper_server_config(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_paper_config()
}

pub(super) async fn set_paper_server_config(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_paper_config()
}

pub(super) async fn get_paper_world_config(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_paper_config()
}

pub(super) async fn set_paper_world_config(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    unsupported_paper_config()
}

pub(super) async fn get_launch_command(State(opanel): State<Arc<OPanel>>) -> Response {
    match opanel.storage().read_text(&LAUNCH_COMMAND_FILE).await {
        Ok(launch_command) => {
            ApiResponse::ok(LaunchCommandPayload { launch_command }).into_response()
        }
        Err(error) => {
            error!(%error, "failed to read launch command");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        }
    }
}

pub(super) async fn set_launch_command(State(opanel): State<Arc<OPanel>>, body: Bytes) -> Response {
    let launch_command = match String::from_utf8(body.to_vec()) {
        Ok(command) => command,
        Err(error) => {
            error!(%error, "launch command is not UTF-8");
            return ApiError::new(StatusCode::BAD_REQUEST, "Launch command must be UTF-8.")
                .into_response();
        }
    };

    match opanel
        .storage()
        .write_text(&LAUNCH_COMMAND_FILE, &launch_command)
        .await
    {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => {
            error!(%error, "failed to write launch command");
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        }
    }
}

fn unsupported_code_of_conduct() -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support server code-of-conduct management.")
}

fn unsupported_paper_config() -> ApiError {
    ApiError::service_unavailable("This server is not a Paper server.")
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
