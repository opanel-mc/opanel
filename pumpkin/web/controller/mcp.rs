use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn get_mcp_enabled(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn toggle_mcp(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn get_masked_access_token(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn generate_access_token(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}
