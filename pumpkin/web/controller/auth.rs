use std::{
    net::{IpAddr, SocketAddr},
    sync::Arc,
};

use axum::{
    extract::{
        ConnectInfo, Query, State,
        rejection::{ExtensionRejection, QueryRejection, StringRejection},
    },
    http::{HeaderMap, HeaderValue, StatusCode, header},
    response::{IntoResponse, Response},
};
use axum_extra::extract::cookie::CookieJar;
use serde::{Deserialize, Serialize};
use tracing::warn;

use crate::{
    opanel::OPanel,
    web::{
        auth::{AuthManager, ChallengeCreateResult, LoginAttemptResult},
        middleware::{TOKEN_COOKIE_NAME, add_token_cookie, remove_token_cookie},
        response::{ApiError, ApiResponse},
    },
};

const NO_STORE: HeaderValue = HeaderValue::from_static("no-store");

#[derive(Debug, Deserialize)]
pub(super) struct ChallengeQuery {
    id: Option<String>,
}

#[derive(Debug, Deserialize)]
struct ValidateRequest {
    id: Option<String>,
    result: Option<String>,
}

#[derive(Debug, Serialize)]
struct ChallengePayload {
    cram: String,
}

#[derive(Debug, Serialize)]
struct EmptyPayload {}

pub(super) async fn get_cram(
    State(opanel): State<Arc<OPanel>>,
    connect_info: Result<ConnectInfo<SocketAddr>, ExtensionRejection>,
    headers: HeaderMap,
    query: Result<Query<ChallengeQuery>, QueryRejection>,
) -> Response {
    let config = opanel.config();
    if !credentials_initialized(&config.access_key, &config.salt) {
        return no_store(ApiError::new(
            StatusCode::SERVICE_UNAVAILABLE,
            "Panel credential is not initialized.",
        ));
    }

    let Ok(Query(ChallengeQuery { id: Some(id) })) = query else {
        return no_store(ApiError::new(StatusCode::BAD_REQUEST, "Id is invalid."));
    };
    if !valid_id(&id) {
        return no_store(ApiError::new(StatusCode::BAD_REQUEST, "Id is invalid."));
    }

    let ip = match checked_client_ip(&opanel, &headers, connect_info.as_ref().ok()) {
        Ok(ip) => ip,
        Err(response) => return with_no_store(*response),
    };

    let response = match opanel.managers().auth().create_challenge(&ip, &id) {
        ChallengeCreateResult::Created { challenge } => {
            ApiResponse::ok(ChallengePayload { cram: challenge }).into_response()
        }
        ChallengeCreateResult::Duplicate => {
            ApiError::new(StatusCode::CONFLICT, "Id is already in use.").into_response()
        }
        ChallengeCreateResult::CapacityFull {
            retry_after_seconds,
        } => with_retry_after(
            ApiError::new(
                StatusCode::TOO_MANY_REQUESTS,
                "Too many login challenges are active.",
            ),
            retry_after_seconds,
        ),
    };
    with_no_store(response)
}

pub(super) async fn validate_cram(
    State(opanel): State<Arc<OPanel>>,
    connect_info: Result<ConnectInfo<SocketAddr>, ExtensionRejection>,
    headers: HeaderMap,
    jar: CookieJar,
    body: Result<String, StringRejection>,
) -> Response {
    let config = opanel.config();
    if !credentials_initialized(&config.access_key, &config.salt) {
        return no_store(ApiError::new(
            StatusCode::SERVICE_UNAVAILABLE,
            "Panel credential is not initialized.",
        ));
    }

    let Ok(body) = body else {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Request body is invalid.",
        ));
    };
    let request: Option<ValidateRequest> = match serde_json::from_str(&body) {
        Ok(request) if !body.trim().is_empty() => request,
        _ => {
            return no_store(ApiError::new(
                StatusCode::BAD_REQUEST,
                "Request body is invalid.",
            ));
        }
    };
    let Some(request) = request else {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Id or result is invalid.",
        ));
    };
    let (Some(id), Some(provided_result)) = (request.id, request.result) else {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Id or result is invalid.",
        ));
    };
    if !valid_id(&id) || !valid_cram_result(&provided_result) {
        return no_store(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Id or result is invalid.",
        ));
    }

    let ip = match checked_client_ip(&opanel, &headers, connect_info.as_ref().ok()) {
        Ok(ip) => ip,
        Err(response) => return with_no_store(*response),
    };

    let Some(challenge) = opanel.managers().auth().consume_challenge(&ip, &id) else {
        return with_no_store(record_failed_login(&opanel, &ip));
    };
    let expected_result = format!(
        "{:x}",
        md5::compute(format!("{}{}", config.access_key, challenge))
    );

    if !AuthManager::verify_cram_result(&provided_result, &expected_result) {
        return with_no_store(record_failed_login(&opanel, &ip));
    }

    opanel.managers().auth().record_login_success(&ip);
    let token = opanel
        .managers()
        .auth()
        .issue_token(&config.access_key, &config.salt);
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
    (
        remove_token_cookie(jar, opanel.config().cookie_secure),
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

fn valid_id(id: &str) -> bool {
    (1..=64).contains(&id.len())
        && id
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'_' | b'-'))
}

fn valid_cram_result(result: &str) -> bool {
    result.len() == 32
        && result
            .bytes()
            .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
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
        ValidateRequest, client_ip, login_throttle_response, no_store, valid_cram_result, valid_id,
        with_no_store, with_retry_after,
    };

    async fn response_json(response: axum::response::Response) -> Value {
        let body = to_bytes(response.into_body(), usize::MAX)
            .await
            .expect("response body should be readable");
        serde_json::from_slice(&body).expect("response body should contain JSON")
    }

    #[test]
    fn validates_cram_fields() {
        assert!(valid_id("browser_1-session"));
        assert!(!valid_id(""));
        assert!(!valid_id(&"a".repeat(65)));
        assert!(!valid_id("contains a space"));

        assert!(valid_cram_result("0123456789abcdef0123456789abcdef"));
        assert!(!valid_cram_result("0123456789ABCDEF0123456789ABCDEF"));
        assert!(!valid_cram_result("too-short"));
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
            .body(Body::from(
                r#"{"id":"browser_1","result":"0123456789abcdef0123456789abcdef"}"#,
            ))
            .unwrap();

        let body = String::from_request(request, &())
            .await
            .expect("text/plain should be extractable as a raw string");
        let parsed: Option<ValidateRequest> =
            serde_json::from_str(&body).expect("raw text should contain valid login JSON");
        let parsed = parsed.expect("a login object should not deserialize as null");

        assert_eq!(parsed.id.as_deref(), Some("browser_1"));
        assert_eq!(
            parsed.result.as_deref(),
            Some("0123456789abcdef0123456789abcdef")
        );
    }

    #[test]
    fn login_json_distinguishes_null_missing_fields_and_malformed_input() {
        let null_request: Option<ValidateRequest> = serde_json::from_str("null").unwrap();
        assert!(null_request.is_none());

        let missing_fields: Option<ValidateRequest> = serde_json::from_str("{}").unwrap();
        let missing_fields = missing_fields.expect("an object should deserialize as a request");
        assert!(missing_fields.id.is_none());
        assert!(missing_fields.result.is_none());

        assert!(serde_json::from_str::<Option<ValidateRequest>>("not-json").is_err());
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
                "Too many login challenges are active.",
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
                "error": "Too many login challenges are active."
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
