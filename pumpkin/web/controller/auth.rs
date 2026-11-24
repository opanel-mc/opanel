use std::{
    net::{IpAddr, SocketAddr},
    sync::Arc,
};

use axum::{
    extract::{
        ConnectInfo, State,
        rejection::{ExtensionRejection, StringRejection},
    },
    http::{HeaderMap, HeaderValue, StatusCode, header},
    response::{IntoResponse, Response},
};
use axum_extra::extract::cookie::CookieJar;
use serde::{Deserialize, Serialize};
use tracing::warn;

use crate::{
    opanel::OPanel,
    utils::access_key::{self, VerifyResult},
    web::{
        auth::LoginAttemptResult,
        middleware::{TOKEN_COOKIE_NAME, add_token_cookie, remove_token_cookie},
        response::{ApiError, ApiResponse},
    },
};

const NO_STORE: HeaderValue = HeaderValue::from_static("no-store");

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct LoginRequest {
    access_key: Option<String>,
}

#[derive(Debug, Serialize)]
struct EmptyPayload {}

pub(super) async fn login(
    State(opanel): State<Arc<OPanel>>,
    connect_info: Result<ConnectInfo<SocketAddr>, ExtensionRejection>,
    headers: HeaderMap,
    jar: CookieJar,
    body: Result<String, StringRejection>,
) -> Response {
    let config = opanel.config();
    if !credentials_initialized(&config.access_key, &config.salt) {
        return no_store(ApiError::service_unavailable(
            "Panel credential is not initialized.",
        ));
    }

    let Ok(body) = body else {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Request body is invalid.",
        ));
    };
    let request: Option<LoginRequest> = match serde_json::from_str(&body) {
        Ok(request) if !body.trim().is_empty() => request,
        _ => {
            return no_store(ApiError::new(
                StatusCode::BAD_REQUEST,
                "Request body is invalid.",
            ));
        }
    };
    let Some(LoginRequest {
        access_key: Some(provided_key),
    }) = request
    else {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Access key is invalid.",
        ));
    };
    if !access_key::is_valid_input_length(&provided_key) {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Access key is invalid.",
        ));
    }

    let ip = match checked_client_ip(&opanel, &headers, connect_info.as_ref().ok()) {
        Ok(ip) => ip,
        Err(response) => return with_no_store(*response),
    };

    let active_access_key = {
        let Some(_slot) = access_key::try_acquire_request_slot() else {
            return with_no_store(with_retry_after(
                ApiError::new(
                    StatusCode::TOO_MANY_REQUESTS,
                    "Too many authentication requests are in progress.",
                ),
                1,
            ));
        };

        let verify_result = access_key::verify(&provided_key, &config.access_key).await;
        if verify_result == VerifyResult::Invalid {
            return with_no_store(record_failed_login(&opanel, &ip));
        }

        opanel.managers().auth().record_login_success(&ip);

        let mut active_access_key = config.access_key.clone();
        if verify_result == VerifyResult::ValidNeedsUpgrade {
            match access_key::hash(&provided_key).await {
                Ok(upgraded) => {
                    let mut replacement = (*config).clone();
                    replacement.access_key = upgraded.clone();
                    if let Err(error) = opanel.managers().config().replace(replacement).await {
                        warn!(%error, "failed to persist upgraded access key hash");
                    } else {
                        active_access_key = upgraded;
                    }
                }
                Err(error) => {
                    warn!(%error, "failed to hash upgraded access key");
                }
            }
        }
        active_access_key
    };

    let token = opanel
        .managers()
        .auth()
        .issue_token(&active_access_key, &config.salt);
    let response = (
        add_token_cookie(jar, token, config.cookie_secure),
        ApiResponse::ok(EmptyPayload {}),
    )
        .into_response();
    with_no_store(response)
}

pub(super) async fn check_auth(State(opanel): State<Arc<OPanel>>, jar: CookieJar) -> Response {
    let config = opanel.config();
    let Some(token) = jar.get(TOKEN_COOKIE_NAME).map(|cookie| cookie.value()) else {
        return ApiError::new(StatusCode::UNAUTHORIZED, "Token is missing.").into_response();
    };
    if !opanel
        .managers()
        .auth()
        .verify_token(token, &config.access_key, &config.salt)
    {
        return (
            remove_token_cookie(jar, config.cookie_secure),
            ApiError::new(StatusCode::UNAUTHORIZED, "Token is invalid."),
        )
            .into_response();
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn logout(State(opanel): State<Arc<OPanel>>, jar: CookieJar) -> Response {
    let config = opanel.config();
    if let Some(token) = jar.get(TOKEN_COOKIE_NAME).map(|cookie| cookie.value()) {
        opanel
            .managers()
            .auth()
            .revoke_token(token, &config.access_key, &config.salt);
    }

    (
        remove_token_cookie(jar, config.cookie_secure),
        ApiResponse::ok(EmptyPayload {}),
    )
        .into_response()
}

fn checked_client_ip(
    opanel: &OPanel,
    headers: &HeaderMap,
    connect_info: Option<&ConnectInfo<SocketAddr>>,
) -> Result<String, Box<Response>> {
    let config = opanel.config();
    let ip = client_ip(headers, connect_info, config.proxy_headers).ok_or_else(|| {
        Box::new(
            ApiError::new(StatusCode::FORBIDDEN, "Cannot determine client IP address.")
                .into_response(),
        )
    })?;

    match opanel.managers().auth().check_login(&ip) {
        LoginAttemptResult::Allowed { .. } => Ok(ip),
        result => Err(Box::new(login_throttle_response(result))),
    }
}

fn record_failed_login(opanel: &OPanel, ip: &str) -> Response {
    match opanel.managers().auth().record_login_failure(ip) {
        LoginAttemptResult::Allowed { failed_attempts } => {
            warn!(%ip, failed_attempts, "failed login request");
            ApiError::from_status(StatusCode::UNAUTHORIZED).into_response()
        }
        result => login_throttle_response(result),
    }
}

fn login_throttle_response(result: LoginAttemptResult) -> Response {
    match result {
        LoginAttemptResult::Allowed { .. } => {
            debug_assert!(false, "allowed login result is not a throttle response");
            ApiError::from_status(StatusCode::UNAUTHORIZED).into_response()
        }
        LoginAttemptResult::Banned { .. } => {
            ApiError::new(StatusCode::FORBIDDEN, "The Ip is banned temporarily.").into_response()
        }
        LoginAttemptResult::CapacityFull {
            retry_after_seconds,
        } => with_retry_after(
            ApiError::new(
                StatusCode::TOO_MANY_REQUESTS,
                "Too many login sources are being tracked.",
            ),
            retry_after_seconds,
        ),
    }
}

fn client_ip(
    headers: &HeaderMap,
    connect_info: Option<&ConnectInfo<SocketAddr>>,
    proxy_headers: bool,
) -> Option<String> {
    if proxy_headers {
        if let Some(ip) = forwarded_for_ip(headers) {
            return Some(ip);
        }
        if let Some(ip) = single_proxy_ip(headers, "x-real-ip") {
            return Some(ip);
        }
    }

    connect_info.map(|ConnectInfo(address)| address.ip().to_string())
}

fn forwarded_for_ip(headers: &HeaderMap) -> Option<String> {
    let value = headers.get("x-forwarded-for")?.to_str().ok()?;
    value.split(',').find_map(parse_proxy_ip)
}

fn single_proxy_ip(headers: &HeaderMap, name: &'static str) -> Option<String> {
    parse_proxy_ip(headers.get(name)?.to_str().ok()?)
}

fn parse_proxy_ip(candidate: &str) -> Option<String> {
    candidate
        .trim()
        .parse::<IpAddr>()
        .ok()
        .map(|ip| ip.to_string())
}

fn credentials_initialized(access_key: &str, salt: &str) -> bool {
    !access_key.trim().is_empty() && !salt.trim().is_empty()
}

fn no_store(response: impl IntoResponse) -> Response {
    with_no_store(response.into_response())
}

fn with_no_store(mut response: Response) -> Response {
    response
        .headers_mut()
        .insert(header::CACHE_CONTROL, NO_STORE);
    response
}

fn with_retry_after(response: impl IntoResponse, seconds: u64) -> Response {
    let mut response = response.into_response();
    if let Ok(value) = HeaderValue::from_str(&seconds.to_string()) {
        response.headers_mut().insert(header::RETRY_AFTER, value);
    }
    response
}

#[cfg(test)]
mod tests {
    use std::net::{IpAddr, Ipv4Addr, SocketAddr};

    use axum::{
        body::{Body, to_bytes},
        extract::{ConnectInfo, FromRequest},
        http::{HeaderMap, Request, StatusCode, header},
    };
    use serde_json::{Value, json};

    use crate::web::{auth::LoginAttemptResult, response::ApiError};

    use super::{
        LoginRequest, client_ip, login_throttle_response, no_store, with_no_store, with_retry_after,
    };

    async fn response_json(response: axum::response::Response) -> Value {
        let body = to_bytes(response.into_body(), usize::MAX)
            .await
            .expect("response body should be readable");
        serde_json::from_slice(&body).expect("response body should contain JSON")
    }

    #[test]
    fn proxy_headers_prefer_the_first_usable_forwarded_address() {
        let mut headers = HeaderMap::new();
        headers.insert(
            "x-forwarded-for",
            "unknown, 203.0.113.7, 198.51.100.2".parse().unwrap(),
        );
        headers.insert("x-real-ip", "192.0.2.9".parse().unwrap());
        let peer = ConnectInfo(SocketAddr::new(IpAddr::V4(Ipv4Addr::LOCALHOST), 3000));

        assert_eq!(
            client_ip(&headers, Some(&peer), true).as_deref(),
            Some("203.0.113.7")
        );
        assert_eq!(
            client_ip(&headers, Some(&peer), false).as_deref(),
            Some("127.0.0.1")
        );

        headers.insert("x-forwarded-for", "unknown, not-an-ip".parse().unwrap());
        assert_eq!(
            client_ip(&headers, Some(&peer), true).as_deref(),
            Some("192.0.2.9")
        );
    }

    #[tokio::test]
    async fn text_plain_body_is_accepted_as_login_json() {
        let request = Request::builder()
            .header(header::CONTENT_TYPE, "text/plain")
            .body(Body::from(r#"{"accessKey":"secret-key"}"#))
            .unwrap();

        let body = String::from_request(request, &())
            .await
            .expect("text/plain should be extractable as a raw string");
        let parsed: Option<LoginRequest> =
            serde_json::from_str(&body).expect("raw text should contain valid login JSON");
        let parsed = parsed.expect("a login object should not deserialize as null");

        assert_eq!(parsed.access_key.as_deref(), Some("secret-key"));
    }

    #[test]
    fn login_json_distinguishes_null_missing_fields_and_malformed_input() {
        let null_request: Option<LoginRequest> = serde_json::from_str("null").unwrap();
        assert!(null_request.is_none());

        let missing_fields: Option<LoginRequest> = serde_json::from_str("{}").unwrap();
        let missing_fields = missing_fields.expect("an object should deserialize as a request");
        assert!(missing_fields.access_key.is_none());

        assert!(serde_json::from_str::<Option<LoginRequest>>("not-json").is_err());
    }

    #[tokio::test]
    async fn no_store_errors_preserve_status_and_api_envelope() {
        let response = no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Request body is invalid.",
        ));

        assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        assert_eq!(
            response.headers().get(header::CACHE_CONTROL),
            Some(&"no-store".parse().unwrap())
        );
        assert_eq!(
            response_json(response).await,
            json!({"code": 400, "error": "Request body is invalid."})
        );
    }

    #[tokio::test]
    async fn capacity_errors_include_retry_after_and_no_store() {
        let response = with_no_store(with_retry_after(
            ApiError::new(
                StatusCode::TOO_MANY_REQUESTS,
                "Too many login sources are being tracked.",
            ),
            60,
        ));

        assert_eq!(response.status(), StatusCode::TOO_MANY_REQUESTS);
        assert_eq!(
            response.headers().get(header::CACHE_CONTROL),
            Some(&"no-store".parse().unwrap())
        );
        assert_eq!(
            response.headers().get(header::RETRY_AFTER),
            Some(&"60".parse().unwrap())
        );
        assert_eq!(
            response_json(response).await,
            json!({
                "code": 429,
                "error": "Too many login sources are being tracked."
            })
        );
    }

    #[tokio::test]
    async fn login_throttle_responses_match_the_http_contract() {
        let banned = login_throttle_response(LoginAttemptResult::Banned {
            retry_after_seconds: 600,
        });
        assert_eq!(banned.status(), StatusCode::FORBIDDEN);
        assert!(banned.headers().get(header::RETRY_AFTER).is_none());
        assert_eq!(
            response_json(banned).await,
            json!({"code": 403, "error": "The Ip is banned temporarily."})
        );

        let capacity = login_throttle_response(LoginAttemptResult::CapacityFull {
            retry_after_seconds: 60,
        });
        assert_eq!(capacity.status(), StatusCode::TOO_MANY_REQUESTS);
        assert_eq!(
            capacity.headers().get(header::RETRY_AFTER),
            Some(&"60".parse().unwrap())
        );
        assert_eq!(
            response_json(capacity).await,
            json!({
                "code": 429,
                "error": "Too many login sources are being tracked."
            })
        );
    }
}
