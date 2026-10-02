use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn get_map_enabled(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support the map feature.")
}

pub(super) async fn toggle_map(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support the map feature.")
}

pub(super) async fn get_available_tiles(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support the map feature.")
}

pub(super) async fn get_tiles_in_range(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support the map feature.")
}

pub(super) async fn get_tiles(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support the map feature.")
}
