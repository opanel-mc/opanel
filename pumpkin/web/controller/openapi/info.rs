use std::sync::Arc;

use axum::Router;

use crate::opanel::OPanel;

use super::super::get_route;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new().route("/info", get_route())
}
