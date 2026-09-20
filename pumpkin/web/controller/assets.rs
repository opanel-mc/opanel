use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{delete_route, get_route, post_route};
pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/assets/{name}", get_route())
        .route("/assets/upload/{name}", post_route())
        .route("/assets/reset/{name}", delete_route())
}
