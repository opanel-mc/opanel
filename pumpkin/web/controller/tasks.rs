use std::sync::Arc;

use axum::{
    body::Bytes,
    extract::{Path, Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD};
use serde::Deserialize;
use serde_json::json;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    scheduled_tasks::TaskError,
    utils::base64::decode_string,
    web::response::{ApiError, ApiResponse},
};

#[derive(Deserialize)]
struct TaskEdit {
    name: String,
    cron: String,
    commands: Vec<String>,
}

#[derive(Deserialize)]
pub(super) struct ToggleQuery {
    enabled: Option<String>,
}

pub(super) async fn get_tasks(State(opanel): State<Arc<OPanel>>) -> Response {
    let mut tasks = opanel.managers().scheduled_tasks().tasks().await;
    for task in &mut tasks {
        task.name = STANDARD.encode(&task.name);
    }
    ApiResponse::ok(json!({"tasks": tasks})).into_response()
}

pub(super) async fn create_task(
    State(opanel): State<Arc<OPanel>>,
    body: Bytes,
) -> Result<Response, ApiError> {
    let task = parse_request(&body, true)?;
    let id = opanel
        .managers()
        .scheduled_tasks()
        .create(task.name, task.cron, task.commands)
        .await
        .map_err(task_error)?;
    Ok(ApiResponse::ok(json!({"taskId": id})).into_response())
}

pub(super) async fn edit_task(
    State(opanel): State<Arc<OPanel>>,
    Path(id): Path<String>,
    body: Bytes,
) -> Result<Response, ApiError> {
    let task = parse_request(&body, false)?;
    opanel
        .managers()
        .scheduled_tasks()
        .edit(&id, task.name, task.cron, task.commands)
        .await
        .map_err(task_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn toggle_task(
    State(opanel): State<Arc<OPanel>>,
    Path(id): Path<String>,
    Query(query): Query<ToggleQuery>,
) -> Result<Response, ApiError> {
    let enabled = query
        .enabled
        .ok_or_else(|| ApiError::new(StatusCode::BAD_REQUEST, "Enabled status is missing."))?;
    opanel
        .managers()
        .scheduled_tasks()
        .set_enabled(&id, enabled == "1")
        .await
        .map_err(task_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn delete_task(
    State(opanel): State<Arc<OPanel>>,
    Path(id): Path<String>,
) -> Result<Response, ApiError> {
    opanel
        .managers()
        .scheduled_tasks()
        .delete(&id)
        .await
        .map_err(task_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

fn parse_request(body: &[u8], create: bool) -> Result<TaskEdit, ApiError> {
    let mut task: TaskEdit = serde_json::from_slice(body)
        .map_err(|error| ApiError::new(StatusCode::BAD_REQUEST, error.to_string()))?;
    // The existing frontend sends Base64 on create, but plain text on edit.
    if create {
        task.name = decode_string(&task.name)
            .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "Invalid Base64 task name."))?;
    }
    Ok(task)
}

fn task_error(error: TaskError) -> ApiError {
    let status = match error {
        TaskError::Cron(_) | TaskError::Commands(_) => StatusCode::BAD_REQUEST,
        TaskError::NotFound(_) => StatusCode::NOT_FOUND,
        _ => StatusCode::INTERNAL_SERVER_ERROR,
    };
    ApiError::new(status, error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn creation_decodes_names_but_editing_preserves_plain_text() {
        let request = json!({"name": STANDARD.encode("每日备份"), "cron": "0 0 * * *", "commands": ["save-all"]});
        let created = parse_request(&serde_json::to_vec(&request).unwrap(), true).unwrap();
        assert_eq!(created.name, "每日备份");
        let request = json!({"name": "重命名", "cron": "0 0 * * *", "commands": []});
        let edited = parse_request(&serde_json::to_vec(&request).unwrap(), false).unwrap();
        assert_eq!(edited.name, "重命名");
        assert!(parse_request(br#"{"name":"!","cron":"* * * * *","commands":[]}"#, true).is_err());
        assert!(parse_request(br#"{"name":"task"}"#, false).is_err());
    }

    #[test]
    fn task_errors_keep_validation_and_missing_ids_distinct() {
        assert_eq!(
            task_error(TaskError::NotFound("missing".into()))
                .into_response()
                .status(),
            StatusCode::NOT_FOUND
        );
        assert_eq!(
            task_error(TaskError::Cron("invalid".into()))
                .into_response()
                .status(),
            StatusCode::BAD_REQUEST
        );
        assert_eq!(
            task_error(TaskError::Commands("invalid".into()))
                .into_response()
                .status(),
            StatusCode::BAD_REQUEST
        );
    }
}
