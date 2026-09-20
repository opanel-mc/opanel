use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_route, get_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_post_route())
        .route("/{save_name}", get_route())
        .route("/{save_name}/tiles-range", post_route())
        .route("/{save_name}/tiles", post_route())
        .fallback(response::not_found)
}
