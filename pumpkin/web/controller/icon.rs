use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn get_favicon(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn upload_favicon(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}
