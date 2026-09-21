use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn download_file(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}
