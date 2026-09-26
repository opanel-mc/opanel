use std::sync::Arc;

use axum::extract::State;

use crate::{opanel::OPanel, web::response::ApiError};

pub(super) async fn update_access_key(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}
