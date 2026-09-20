use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::post_route;
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", post_route())
        .fallback(response::not_found)
}
