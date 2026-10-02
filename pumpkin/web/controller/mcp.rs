use std::sync::Arc;

use axum::{
    extract::{Query, State},
    http::{StatusCode, header},
    response::{IntoResponse, Response},
};
use serde::{Deserialize, Serialize};
use serde_json::json;
use subtle::ConstantTimeEq;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    storage::{JsonFile, Storage},
    utils::random::alphanumeric,
    web::response::{ApiError, ApiResponse},
};

const CONFIG: JsonFile<McpConfig> = JsonFile::new("mcp-config.json", McpConfig::default);

#[derive(Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
struct McpConfig {
    enabled: bool,
    access_token: Option<String>,
}

#[derive(Deserialize)]
pub(super) struct ToggleQuery {
    enabled: Option<String>,
}

pub(super) async fn get_mcp_enabled(
    State(opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let config = opanel
        .storage()
        .load_json(&CONFIG)
        .await
        .map_err(internal_error)?
        .value;
    Ok(ApiResponse::ok(json!({"enabled": config.enabled})).into_response())
}

pub(super) async fn toggle_mcp(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<ToggleQuery>,
) -> Result<Response, ApiError> {
    let enabled = query
        .enabled
        .ok_or_else(|| ApiError::new(StatusCode::BAD_REQUEST, "Status is missing."))?;
    set_enabled(&opanel.storage(), enabled == "1").await?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn get_masked_access_token(
    State(opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let config = opanel
        .storage()
        .load_json(&CONFIG)
        .await
        .map_err(internal_error)?
        .value;
    let masked = mask_token(config.access_token.as_deref())?;
    Ok((
        [(header::CACHE_CONTROL, "no-store")],
        ApiResponse::ok(json!({"maskedAccessToken": masked})),
    )
        .into_response())
}

pub(super) async fn generate_access_token(
    State(opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let token = rotate_token(&opanel.storage()).await?;
    Ok((
        [(header::CACHE_CONTROL, "no-store")],
        ApiResponse::ok(json!({"accessToken": token})),
    )
        .into_response())
}

async fn set_enabled(storage: &Storage, enabled: bool) -> Result<(), ApiError> {
    if enabled
        && storage
            .load_json(&CONFIG)
            .await
            .map_err(internal_error)?
            .value
            .access_token
            .is_none()
    {
        return Err(ApiError::new(
            StatusCode::FORBIDDEN,
            "Access token is not set.",
        ));
    }
    storage
        .update_json(&CONFIG, |config| {
            config.enabled = enabled;
        })
        .await
        .map_err(internal_error)
}

async fn rotate_token(storage: &Storage) -> Result<String, ApiError> {
    let token = format!("o-{}", alphanumeric(48).map_err(internal_error)?);
    storage
        .update_json(&CONFIG, |config| {
            config.access_token = Some(token.clone());
        })
        .await
        .map_err(internal_error)?;
    Ok(token)
}

pub(in crate::web) fn validate_token_format(token: &str) -> Result<(), ApiError> {
    if token.starts_with("o-") && token.len() == 50 {
        Ok(())
    } else {
        Err(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Authorization header is invalid.",
        ))
    }
}

pub(in crate::web) async fn authenticate(storage: &Storage, token: &str) -> Result<(), ApiError> {
    validate_token_format(token)?;
    let config = storage
        .load_json(&CONFIG)
        .await
        .map_err(internal_error)?
        .value;
    if !config.enabled {
        return Err(ApiError::service_unavailable("Mcp is not enabled."));
    }
    if !config
        .access_token
        .as_deref()
        .is_some_and(|expected| bool::from(expected.as_bytes().ct_eq(token.as_bytes())))
    {
        return Err(ApiError::new(
            StatusCode::UNAUTHORIZED,
            "Mcp access token is invalid.",
        ));
    }
    Ok(())
}

fn mask_token(token: Option<&str>) -> Result<Option<String>, ApiError> {
    token
        .map(|token| {
            // Java indexes strings by UTF-16 units and permits the two slices to overlap.
            let units: Vec<_> = token.encode_utf16().collect();
            if units.len() < 5 {
                // Java's out-of-bounds exception becomes an HTTP 500 via WebServer.
                return Err(internal_error("Stored access token is too short to mask."));
            }
            let mut masked = units[..4].to_vec();
            masked.extend("***".encode_utf16());
            masked.extend_from_slice(&units[units.len() - 5..]);
            Ok(String::from_utf16_lossy(&masked))
        })
        .transpose()
}

fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn masking_matches_java_null_slices_and_short_token_errors() {
        assert_eq!(mask_token(None).unwrap(), None);
        for (token, expected) in [
            (format!("o-{}12345", "a".repeat(43)), "o-aa***12345"),
            ("abcde".into(), "abcd***abcde"),
            ("abcdefgh".into(), "abcd***defgh"),
            ("😀abc".into(), "😀ab***😀abc"),
        ] {
            assert_eq!(mask_token(Some(&token)).unwrap().as_deref(), Some(expected));
        }
        for token in ["", "o-", "abcd", "😀ab"] {
            let response = mask_token(Some(token)).unwrap_err().into_response();
            assert_eq!(response.status(), StatusCode::INTERNAL_SERVER_ERROR);
            let body = axum::body::to_bytes(response.into_body(), usize::MAX)
                .await
                .unwrap();
            let body: serde_json::Value = serde_json::from_slice(&body).unwrap();
            assert_eq!(body["code"], 500);
            assert!(body["error"].is_string());
        }
    }

    #[tokio::test]
    async fn token_rotation_and_switches_immediately_change_authentication() {
        let root =
            crate::utils::file::random_temporary_path(&std::env::temp_dir(), "mcp-test").unwrap();
        let storage = Storage::open(root.clone()).await.unwrap();
        assert_eq!(
            set_enabled(&storage, true)
                .await
                .unwrap_err()
                .into_response()
                .status(),
            StatusCode::FORBIDDEN
        );
        assert!(!root.join("mcp-config.json").exists());
        let first = rotate_token(&storage).await.unwrap();
        assert_eq!(first.len(), 50);
        assert!(first.starts_with("o-"));
        assert!(first[2..].bytes().all(|byte| byte.is_ascii_alphanumeric()));
        assert_eq!(
            authenticate(&storage, &first)
                .await
                .unwrap_err()
                .into_response()
                .status(),
            StatusCode::SERVICE_UNAVAILABLE
        );
        set_enabled(&storage, true).await.unwrap();
        assert!(authenticate(&storage, &first).await.is_ok());
        let second = rotate_token(&storage).await.unwrap();
        assert_ne!(first, second);
        assert_eq!(
            authenticate(&storage, &first)
                .await
                .unwrap_err()
                .into_response()
                .status(),
            StatusCode::UNAUTHORIZED
        );
        assert!(authenticate(&storage, &second).await.is_ok());
        let reopened = Storage::open(root.clone()).await.unwrap();
        assert!(authenticate(&reopened, &second).await.is_ok());
        set_enabled(&storage, false).await.unwrap();
        assert_eq!(
            authenticate(&storage, &second)
                .await
                .unwrap_err()
                .into_response()
                .status(),
            StatusCode::SERVICE_UNAVAILABLE
        );
        tokio::fs::remove_dir_all(root).await.unwrap();
    }

    #[test]
    fn malformed_bearer_tokens_are_bad_requests() {
        for token in [
            "",
            "o-short",
            &"a".repeat(50),
            &format!("o-{}", "a".repeat(49)),
        ] {
            assert_eq!(
                validate_token_format(token)
                    .unwrap_err()
                    .into_response()
                    .status(),
                StatusCode::BAD_REQUEST
            );
        }
    }
}
