use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_route, post_patch_delete_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_post_route())
        .route("/{id}", post_patch_delete_route())
        .fallback(response::not_found)
}
