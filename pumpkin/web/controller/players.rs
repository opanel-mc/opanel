use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_delete_route, get_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_delete_route())
        .route("/list", get_route())
        .route("/op", post_route())
        .route("/deop", post_route())
        .route("/kick", post_route())
        .route("/ban", post_route())
        .route("/pardon", post_route())
        .route("/gamemode", post_route())
        .fallback(response::not_found)
}
