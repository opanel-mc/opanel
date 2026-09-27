use std::sync::Arc;

use axum::{
    extract::{State, rejection::StringRejection},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use axum_extra::extract::cookie::CookieJar;
use serde::{Deserialize, Serialize};
use subtle::ConstantTimeEq;
use tracing::error;

use crate::{
    opanel::OPanel,
    web::{
        middleware::add_token_cookie,
        response::{ApiError, ApiResponse},
    },
};

#[derive(Debug, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
struct UpdateAccessKeyRequest {
    current_key: String,
    new_key: String,
}

#[derive(Debug, Serialize)]
struct EmptyPayload {}

pub(super) async fn update_access_key(
    State(opanel): State<Arc<OPanel>>,
    jar: CookieJar,
    body: Result<String, StringRejection>,
) -> Response {
    let Some(request) = parse_request(body) else {
        return ApiError::new(StatusCode::BAD_REQUEST, "Invalid request body.").into_response();
    };
    let current_config = opanel.config();
    if !access_key_matches(&request.current_key, &current_config.access_key) {
        return ApiError::new(StatusCode::FORBIDDEN, "Access key mismatch.").into_response();
    }

    let mut replacement = (*current_config).clone();
    replacement.access_key = hash_once(&request.new_key);
    if let Err(error) = opanel.managers().config().replace(replacement).await {
        error!(%error, "failed to persist the replacement access key");
        return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response();
    }

    let config = opanel.config();
    let auth = opanel.managers().auth();
    auth.revoke_all_tokens();
    let token = auth.issue_token(&config.access_key, &config.salt);
    (
        add_token_cookie(jar, token, config.cookie_secure),
        ApiResponse::ok(EmptyPayload {}),
    )
        .into_response()
}

fn parse_request(body: Result<String, StringRejection>) -> Option<UpdateAccessKeyRequest> {
    let body = body.ok()?;
    if body.trim().is_empty() {
        return None;
    }
    serde_json::from_str::<Option<UpdateAccessKeyRequest>>(&body)
        .ok()
        .flatten()
}

fn access_key_matches(current_key: &str, stored_key: &str) -> bool {
    hash_once(current_key)
        .as_bytes()
        .ct_eq(stored_key.as_bytes())
        .into()
}

fn hash_once(value: &str) -> String {
    format!("{:x}", md5::compute(value))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn body(value: &str) -> Result<String, StringRejection> {
        Ok(value.to_string())
    }

    #[test]
    fn request_body_requires_a_json_object_and_both_keys() {
        assert_eq!(
            parse_request(body(r#"{"currentKey":"old","newKey":"new"}"#)),
            Some(UpdateAccessKeyRequest {
                current_key: "old".to_string(),
                new_key: "new".to_string(),
            })
        );
        for invalid in ["", "null", "{}", "[]", "not-json"] {
            assert!(
                parse_request(body(invalid)).is_none(),
                "{invalid:?} should be rejected"
            );
        }
    }

    #[test]
    fn access_key_verification_uses_the_frontend_and_storage_hash_layers() {
        let plaintext = "correct horse battery staple";
        let frontend_hash = hash_once(plaintext);
        let stored_hash = hash_once(&frontend_hash);

        assert!(access_key_matches(&frontend_hash, &stored_hash));
        assert!(!access_key_matches(&hash_once("wrong"), &stored_hash));
        assert_eq!(hash_once(&frontend_hash), stored_hash);
    }
}
