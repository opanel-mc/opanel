use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::{get_post_delete_route, get_route, post_route};
use crate::web::response;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/login", get_route())
        .route("/callback", get_route())
        .route("/bind-user", post_route())
        .route("/config", get_route())
        .route("/allowed-users", get_post_delete_route())
        .fallback(response::not_found)
}
