use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(in crate::web::controller) async fn get_log_file_list(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::not_implemented()
}

pub(in crate::web::controller) async fn get_log_content(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::not_implemented()
}

pub(in crate::web::controller) async fn download_log(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::not_implemented()
}
