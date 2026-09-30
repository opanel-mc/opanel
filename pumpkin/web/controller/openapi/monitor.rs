use std::sync::Arc;

use axum::extract::State;

use crate::{monitor::MonitorData, opanel::OPanel, web::response::ApiResponse};

pub(in crate::web::controller) async fn get_monitor(
    state: State<Arc<OPanel>>,
) -> ApiResponse<MonitorData> {
    super::super::monitor::get_monitor_snapshot(state).await
}
