use std::sync::Arc;

use axum::{Json, Router, extract::State, routing::get};
use serde::Serialize;

use crate::opanel::OPanel;

#[derive(Serialize)]
struct HelloResponse {
    code: u16,
    error: &'static str,
    message: &'static str,
}

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new().route("/hello", get(hello))
}

async fn hello(State(_opanel): State<Arc<OPanel>>) -> Json<HelloResponse> {
    Json(HelloResponse {
        code: 200,
        error: "",
        message: "Hello, world!",
    })
}

#[cfg(test)]
mod tests {
    use axum::response::IntoResponse;

    use super::HelloResponse;

    #[test]
    fn hello_response_has_the_expected_shape() {
        let value = serde_json::to_value(HelloResponse {
            code: 200,
            error: "",
            message: "Hello, world!",
        })
        .expect("hello response should serialize");

        assert_eq!(
            value,
            serde_json::json!({
                "code": 200,
                "error": "",
                "message": "Hello, world!"
            })
        );

        let _ = axum::Json(value).into_response();
    }
}
