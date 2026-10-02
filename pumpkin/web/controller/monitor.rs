use std::sync::Arc;

use axum::extract::State;

use crate::{
    monitor::MonitorData,
    opanel::OPanel,
    web::response::{ApiError, ApiResponse},
};

pub(super) async fn get_monitor_snapshot(
    State(opanel): State<Arc<OPanel>>,
) -> ApiResponse<MonitorData> {
    ApiResponse::ok(opanel.managers().monitor().snapshot())
}

pub(super) async fn get_history(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support monitor history.")
}

pub(super) async fn get_activity(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not support player activity history.")
}
