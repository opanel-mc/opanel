use std::{
    path::{Component, Path},
    process::Command,
    sync::Arc,
};

use axum::{
    body::Bytes,
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64_STANDARD};
use pumpkin::data::datapack::DatapackManager;
use serde::{Deserialize, Serialize};
use tokio::fs;
use toml_edit::{DocumentMut, Item, value};
use tracing::error;

use crate::{
    opanel::OPanel,
    storage::TextFile,
    web::response::{ApiError, ApiResponse},
};

const PUMPKIN_CONFIG_PATH: &str = "pumpkin.toml";
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
    match fs::read(PUMPKIN_CONFIG_PATH).await {
        Ok(properties) => ApiResponse::ok(PropertiesPayload {
            properties: BASE64_STANDARD.encode(properties),
        })
        .into_response(),
        Err(error) => {
            error!(%error, path = PUMPKIN_CONFIG_PATH, "failed to read Pumpkin configuration");
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
    if let Err(error) = configuration.parse::<DocumentMut>() {
        error!(%error, "invalid Pumpkin configuration");
        return ApiError::new(StatusCode::BAD_REQUEST, "Invalid Pumpkin configuration.")
            .into_response();
    }

    match fs::write(PUMPKIN_CONFIG_PATH, configuration).await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => {
            error!(%error, path = PUMPKIN_CONFIG_PATH, "failed to write Pumpkin configuration");
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
    State(_opanel): State<Arc<OPanel>>,
    Query(query): Query<SwitchSaveQuery>,
) -> Response {
    let Some(save_name) = query.save else {
        return ApiError::new(StatusCode::BAD_REQUEST, "Save name is missing.").into_response();
    };
    if !is_safe_file_name(&save_name) {
        return ApiError::new(StatusCode::BAD_REQUEST, "Illegal save name.").into_response();
    }

    if !Path::new(&save_name).join("level.dat").is_file() {
        return ApiError::new(StatusCode::NOT_FOUND, "Cannot find the save.").into_response();
    }

    match update_current_save(PUMPKIN_CONFIG_PATH, &save_name).await {
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
    ApiError::new(
        StatusCode::SERVICE_UNAVAILABLE,
        "Pumpkin does not support server code-of-conduct management.",
    )
}

fn unsupported_paper_config() -> ApiError {
    ApiError::new(
        StatusCode::SERVICE_UNAVAILABLE,
        "This server is not a Paper server.",
    )
}

fn is_safe_file_name(file_name: &str) -> bool {
    let path = Path::new(file_name);
    let mut components = path.components();
    !file_name.is_empty()
        && !file_name.contains(['/', '\\', '\0'])
        && matches!(components.next(), Some(Component::Normal(_)))
        && components.next().is_none()
}

async fn update_current_save(path: &str, save_name: &str) -> Result<(), std::io::Error> {
    let contents = fs::read_to_string(path).await?;
    let updated = set_default_level_name(&contents, save_name)
        .map_err(|error| std::io::Error::new(std::io::ErrorKind::InvalidData, error))?;
    fs::write(path, updated).await
}

fn set_default_level_name(contents: &str, save_name: &str) -> Result<String, toml_edit::TomlError> {
    let mut document = contents.parse::<DocumentMut>()?;
    let level_name = document
        .as_table_mut()
        .entry("default_level_name")
        .or_insert_with(|| value(save_name));
    let decor = level_name
        .as_value()
        .map(|existing_value| existing_value.decor().clone());
    *level_name = Item::Value(save_name.into());
    if let (Some(decor), Some(updated_value)) = (decor, level_name.as_value_mut()) {
        *updated_value.decor_mut() = decor;
    }
    Ok(document.to_string())
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
    fn save_names_must_be_single_safe_path_components() {
        for valid in ["world", "My World", "世界-1"] {
            assert!(is_safe_file_name(valid), "{valid:?} should be valid");
        }
        for invalid in ["", ".", "..", "../world", "world/nether", "world\\nether"] {
            assert!(!is_safe_file_name(invalid), "{invalid:?} should be invalid");
        }
    }

    #[test]
    fn changing_the_current_save_preserves_other_pumpkin_settings() {
        let source = r#"# server
default_level_name = "world" # keep this comment

[networking.java]
motd = "Hello"
"#;

        let updated = set_default_level_name(source, "new world").unwrap();
        let document = updated.parse::<DocumentMut>().unwrap();

        assert_eq!(document["default_level_name"].as_str(), Some("new world"));
        assert_eq!(
            document["networking"]["java"]["motd"].as_str(),
            Some("Hello")
        );
        assert!(updated.contains("# keep this comment"));
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
