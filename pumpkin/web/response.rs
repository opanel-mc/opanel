use std::borrow::Cow;

use axum::{
    Json,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde::Serialize;

#[derive(Debug, Serialize)]
pub struct ApiResponse<T> {
    code: u16,
    error: Cow<'static, str>,
    #[serde(flatten)]
    payload: T,
}

impl<T> ApiResponse<T> {
    #[allow(dead_code)]
    pub fn ok(payload: T) -> Self {
        Self {
            code: StatusCode::OK.as_u16(),
            error: Cow::Borrowed(""),
            payload,
        }
    }
}

impl<T> IntoResponse for ApiResponse<T>
where
    T: Serialize,
{
    fn into_response(self) -> Response {
        (StatusCode::OK, Json(self)).into_response()
    }
}

#[derive(Debug)]
pub struct ApiError {
    status: StatusCode,
    message: String,
}

#[derive(Serialize)]
struct EmptyPayload {}

impl ApiError {
    pub fn new(status: StatusCode, message: impl Into<String>) -> Self {
        Self {
            status,
            message: message.into(),
        }
    }

    pub fn not_found() -> Self {
        Self::new(StatusCode::NOT_FOUND, "Not Found")
    }

    pub fn method_not_allowed() -> Self {
        Self::new(StatusCode::METHOD_NOT_ALLOWED, "Method Not Allowed")
    }

    pub fn not_implemented() -> Self {
        Self::new(StatusCode::NOT_IMPLEMENTED, "Not Implemented")
    }

    pub fn from_status(status: StatusCode) -> Self {
        Self::new(status, status.canonical_reason().unwrap_or_default())
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let body = ApiResponse {
            code: self.status.as_u16(),
            error: Cow::Owned(self.message),
            payload: EmptyPayload {},
        };
        (self.status, Json(body)).into_response()
    }
}

pub async fn not_found() -> ApiError {
    ApiError::not_found()
}

pub async fn method_not_allowed() -> ApiError {
    ApiError::method_not_allowed()
}

#[cfg(test)]
mod tests {
    use axum::{body::to_bytes, response::IntoResponse};
    use serde::Serialize;

    use super::{ApiError, ApiResponse};

    #[derive(Serialize)]
    #[serde(rename_all = "camelCase")]
    struct Payload {
        message: &'static str,
        item_count: u32,
    }

    #[test]
    fn success_response_flattens_its_payload() {
        let value = serde_json::to_value(ApiResponse::ok(Payload {
            message: "Hello, world!",
            item_count: 2,
        }))
        .expect("API response should serialize");

        assert_eq!(
            value,
            serde_json::json!({
                "code": 200,
                "error": "",
                "message": "Hello, world!",
                "itemCount": 2
            })
        );
    }

    #[tokio::test]
    async fn error_response_uses_the_same_envelope() {
        let response = ApiError::not_found().into_response();
        assert_eq!(response.status(), axum::http::StatusCode::NOT_FOUND);

        let body = to_bytes(response.into_body(), usize::MAX)
            .await
            .expect("error response body should be readable");
        assert_eq!(
            serde_json::from_slice::<serde_json::Value>(&body).unwrap(),
            serde_json::json!({"code": 404, "error": "Not Found"})
        );
    }
}
