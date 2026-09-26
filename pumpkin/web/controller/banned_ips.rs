use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn get_banned_ips(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn ban_ip(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

pub(super) async fn pardon_ip(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}
