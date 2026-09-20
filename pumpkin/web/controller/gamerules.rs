use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::get_post_patch_route;
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/{dim_name}", get_post_patch_route())
        .fallback(response::not_found)
}
