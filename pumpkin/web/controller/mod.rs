use std::sync::Arc;

use axum::{Router, routing::any};

use crate::opanel::OPanel;

use super::response;

mod hello;

pub fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .merge(hello::router())
        .fallback(response::not_found)
        .route("/", any(response::not_found))
}
