use std::{
    path::Path,
    sync::{Arc, LazyLock},
    time::Duration,
};

use axum::{
    extract::{Path as AxumPath, State},
    http::{StatusCode, header},
    response::{IntoResponse, Response},
};
use percent_encoding::{NON_ALPHANUMERIC, utf8_percent_encode};
use pumpkin_data::packet::CURRENT_MC_VERSION;
use serde::Deserialize;
use serde_json::json;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    storage::TMP_DIR_NAME,
    utils::{
        file::random_temporary_path,
        logs::{self, LogError},
    },
    web::response::{ApiError, ApiResponse},
};

const LOG_DIRECTORY: &str = "logs";
const MCLOGS_URL: &str = "https://api.mclo.gs/1/log";
static HTTP_CLIENT: LazyLock<Result<reqwest::Client, reqwest::Error>> = LazyLock::new(|| {
    // Native plugins have their own statically linked TLS state, separate from the host.
    let _ = rustls::crypto::ring::default_provider().install_default();
    reqwest::Client::builder()
        .connect_timeout(Duration::from_secs(10))
        .timeout(Duration::from_secs(30))
        .build()
});

pub(super) async fn get_log_file_list(
    State(_opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let files = tokio::task::spawn_blocking(|| logs::list(Path::new(LOG_DIRECTORY)))
        .await
        .map_err(internal_error)?
        .map_err(log_error)?;
    Ok(ApiResponse::ok(json!({"logs": files})).into_response())
}

pub(super) async fn get_log_content(
    State(_opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<Response, ApiError> {
    Ok((
        [(header::CONTENT_TYPE, "text/plain; charset=utf-8")],
        read_log(name).await?,
    )
        .into_response())
}

pub(super) async fn download_log(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<Response, ApiError> {
    let content = read_log(name.clone()).await?;
    let directory = opanel.storage().root().join(TMP_DIR_NAME);
    tokio::fs::create_dir_all(&directory)
        .await
        .map_err(internal_error)?;
    let temporary = random_temporary_path(&directory, "log").map_err(internal_error)?;
    if let Err(error) = tokio::fs::write(&temporary, content).await {
        let _ = tokio::fs::remove_file(&temporary).await;
        return Err(internal_error(error));
    }
    let id = match opanel
        .downloads()
        .register_path(temporary.clone(), true)
        .await
    {
        Ok(id) => id,
        Err(error) => {
            let _ = tokio::fs::remove_file(temporary).await;
            return Err(internal_error(error));
        }
    };
    let downloaded_name = name
        .strip_suffix(".log.gz")
        .map_or_else(|| name.clone(), |stem| format!("{stem}.log"));
    let location = format!(
        "/file/{id}/{}",
        utf8_percent_encode(&downloaded_name, NON_ALPHANUMERIC)
    );
    Ok((StatusCode::FOUND, [(header::LOCATION, location)]).into_response())
}

pub(super) async fn clear_logs(State(_opanel): State<Arc<OPanel>>) -> Result<Response, ApiError> {
    tokio::task::spawn_blocking(|| logs::clear(Path::new(LOG_DIRECTORY)))
        .await
        .map_err(internal_error)?
        .map_err(log_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn delete_log(
    State(_opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<Response, ApiError> {
    tokio::task::spawn_blocking(move || logs::delete(Path::new(LOG_DIRECTORY), &name))
        .await
        .map_err(internal_error)?
        .map_err(log_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn upload_log_to_mclogs(
    State(_opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<Response, ApiError> {
    let content = read_log(name).await?;
    let client = HTTP_CLIENT.as_ref().map_err(internal_error)?;
    let id = upload(client, MCLOGS_URL, content).await?;
    Ok(ApiResponse::ok(json!({"id": id})).into_response())
}

async fn read_log(name: String) -> Result<String, ApiError> {
    tokio::task::spawn_blocking(move || logs::read(Path::new(LOG_DIRECTORY), &name))
        .await
        .map_err(internal_error)?
        .map_err(log_error)
}

async fn upload(client: &reqwest::Client, url: &str, content: String) -> Result<String, ApiError> {
    let response = client.post(url).header(header::ACCEPT, "application/json").json(&json!({
        "content": content,
        "source": "OPanel",
        "metadata": [
            {"key": "server_software", "value": "Pumpkin", "label": "Server Software"},
            {"key": "mc_version", "value": CURRENT_MC_VERSION.to_string(), "label": "Minecraft Version"},
            {"key": "opanel_version", "value": OPanel::VERSION, "label": "OPanel Version"}
        ]
    })).send().await.map_err(|_| gateway_error("Failed to connect to mclo.gs."))?;
    let success = response.status().is_success();
    let body = response
        .bytes()
        .await
        .map_err(|_| gateway_error("Failed to read the mclo.gs response."))?;
    parse_upload_response(success, &body)
}

fn parse_upload_response(http_success: bool, body: &[u8]) -> Result<String, ApiError> {
    #[derive(Deserialize)]
    struct UploadResponse {
        #[serde(default)]
        success: bool,
        error: Option<String>,
        id: Option<String>,
    }
    let body: UploadResponse = serde_json::from_slice(body)
        .map_err(|_| gateway_error("mclo.gs returned an invalid response."))?;
    if !http_success || !body.success {
        return Err(gateway_error(
            body.error
                .unwrap_or_else(|| "mclo.gs rejected the log upload.".into()),
        ));
    }
    body.id
        .filter(|id| !id.is_empty())
        .ok_or_else(|| gateway_error("mclo.gs did not return a log ID."))
}

fn log_error(error: LogError) -> ApiError {
    let status = match &error {
        LogError::InvalidName => StatusCode::BAD_REQUEST,
        LogError::ActiveLog => StatusCode::FORBIDDEN,
        LogError::Io(error) if error.kind() == std::io::ErrorKind::NotFound => {
            return ApiError::new(StatusCode::NOT_FOUND, "Cannot find the specified log file.");
        }
        LogError::Io(_) => StatusCode::INTERNAL_SERVER_ERROR,
    };
    ApiError::new(status, error.to_string())
}

fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}
fn gateway_error(message: impl Into<String>) -> ApiError {
    ApiError::new(StatusCode::BAD_GATEWAY, message)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn upload_requires_successful_http_json_and_id() {
        assert_eq!(
            parse_upload_response(true, br#"{"success":true,"id":"test"}"#).unwrap(),
            "test"
        );
        for (status, body) in [
            (false, r#"{"success":true,"id":"test"}"#),
            (true, r#"{"success":false,"error":"Rejected"}"#),
            (true, r#"{"success":true}"#),
            (true, r#"{"success":true,"id":42}"#),
            (true, "not json"),
        ] {
            assert_eq!(
                parse_upload_response(status, body.as_bytes())
                    .unwrap_err()
                    .into_response()
                    .status(),
                StatusCode::BAD_GATEWAY
            );
        }
    }

    #[tokio::test]
    async fn upload_sends_log_and_metadata_to_the_api() {
        use axum::{Json, Router, routing::post};
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let app = Router::new().route(
            "/1/log",
            post(|Json(body): Json<serde_json::Value>| async move {
                assert_eq!(body["content"], "test log");
                assert_eq!(body["source"], "OPanel");
                assert_eq!(body["metadata"][0]["value"], "Pumpkin");
                Json(json!({"success": true, "id": "test-id"}))
            }),
        );
        let server = tokio::spawn(async move {
            axum::serve(listener, app).await.unwrap();
        });
        HTTP_CLIENT.as_ref().unwrap();
        let client = reqwest::Client::builder().no_proxy().build().unwrap();
        let result = upload(
            &client,
            &format!("http://{address}/1/log"),
            "test log".into(),
        )
        .await;
        server.abort();
        assert_eq!(result.unwrap(), "test-id");
    }
}
