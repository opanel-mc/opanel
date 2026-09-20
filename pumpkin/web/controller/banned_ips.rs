use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_route())
        .route("/add", post_route())
        .route("/remove", post_route())
        .fallback(response::not_found)
}
