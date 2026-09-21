use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(in crate::web::controller) async fn get_plugins(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::not_implemented()
}

pub(in crate::web::controller) async fn get_plugin_icon(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::not_implemented()
}
