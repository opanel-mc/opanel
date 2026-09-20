use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_delete_route, get_post_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/properties", get_post_route())
        .route("/code-of-conduct", get_post_delete_route())
        .route("/stop", post_route())
        .route("/reload", post_route())
        .route("/restart", post_route())
        .route("/world", post_route())
        .route("/paper-config", get_post_route())
        .route("/paper-world-config", get_post_route())
        .route("/launch-command", get_post_route())
        .fallback(response::not_found)
}
