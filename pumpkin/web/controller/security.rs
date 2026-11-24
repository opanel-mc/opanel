use std::sync::Arc;

use axum::{
    extract::{State, rejection::StringRejection},
    http::{HeaderValue, StatusCode, header},
    response::{IntoResponse, Response},
};
use axum_extra::extract::cookie::CookieJar;
use serde::{Deserialize, Serialize};
use tracing::error;

use crate::{
    opanel::OPanel,
    utils::access_key::{self, VerifyResult},
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
    let hashed_new_key = {
        let Some(_slot) = access_key::try_acquire_request_slot() else {
            let mut response = ApiError::new(
                StatusCode::TOO_MANY_REQUESTS,
                "Too many authentication requests are in progress.",
            )
            .into_response();
            response
                .headers_mut()
                .insert(header::RETRY_AFTER, HeaderValue::from_static("1"));
            return response;
        };

        if access_key::verify(&request.current_key, &current_config.access_key).await
            == VerifyResult::Invalid
        {
            return ApiError::new(StatusCode::FORBIDDEN, "Access key mismatch.").into_response();
        }

        match access_key::hash(&request.new_key).await {
            Ok(hashed) => hashed,
            Err(error) => {
                error!(%error, "failed to hash the replacement access key");
                return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
                    .into_response();
            }
        }
    };

    let mut replacement = (*current_config).clone();
    replacement.access_key = hashed_new_key;
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
    let request = serde_json::from_str::<Option<UpdateAccessKeyRequest>>(&body)
        .ok()
        .flatten()?;
    if !access_key::is_valid_input_length(&request.current_key)
        || !access_key::is_valid_new_key_length(&request.new_key)
    {
        return None;
    }
    Some(request)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn body(value: &str) -> Result<String, StringRejection> {
        Ok(value.to_string())
    }

    #[test]
    fn request_body_requires_a_json_object_and_valid_key_lengths() {
        assert_eq!(
            parse_request(body(r#"{"currentKey":"old","newKey":"new-key"}"#)),
            Some(UpdateAccessKeyRequest {
                current_key: "old".to_string(),
                new_key: "new-key".to_string(),
            })
        );
        let long_legacy_key = "a".repeat(300);
        assert_eq!(
            parse_request(body(&format!(
                r#"{{"currentKey":"{long_legacy_key}","newKey":"new-key"}}"#
            ))),
            Some(UpdateAccessKeyRequest {
                current_key: long_legacy_key,
                new_key: "new-key".to_string(),
            })
        );
        let too_long_new_key = "b".repeat(257);
        for invalid in [
            "",
            "null",
            "{}",
            "[]",
            "not-json",
            r#"{"currentKey":"","newKey":"new-key"}"#,
            r#"{"currentKey":"old","newKey":"short"}"#,
            &format!(r#"{{"currentKey":"old","newKey":"{too_long_new_key}"}}"#),
        ] {
            assert!(
                parse_request(body(invalid)).is_none(),
                "{invalid:?} should be rejected"
            );
        }
    }
}
