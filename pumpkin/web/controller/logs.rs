use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_delete_route, get_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/", get_delete_route())
        .route("/{file_name}", get_delete_route())
        .route("/{file_name}/download", get_route())
        .route("/{file_name}/upload-mclogs", post_route())
        .fallback(response::not_found)
}
