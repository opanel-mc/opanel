use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_delete_route, get_post_route, get_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_post_route())
        .route("/icon/{file_name}", get_route())
        .route("/{file_name}", get_post_delete_route())
        .fallback(response::not_found)
}
