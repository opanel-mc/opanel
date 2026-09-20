use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_post_route())
        .route("/check", post_route())
        .route("/logout", post_route())
        .fallback(response::not_found)
}
