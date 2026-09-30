use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn get_plugins(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}

pub(super) async fn get_plugin_icon(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}

pub(super) async fn upload_plugin(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}

pub(super) async fn download_plugin(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}

pub(super) async fn toggle_plugin(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}

pub(super) async fn delete_plugin(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support plugin management.")
}
