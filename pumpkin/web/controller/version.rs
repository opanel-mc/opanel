use std::sync::Arc;

use axum::extract::State;
use pumpkin_data::packet::CURRENT_MC_VERSION;
use serde::Serialize;

use crate::{opanel::OPanel, web::response::ApiResponse};

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct VersionInfo {
    version: String,
    map: bool,
    mcdr: bool,
    monitor_history_enabled: bool,
    code_of_conduct: bool,
}

pub(super) async fn get_version_info(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiResponse<VersionInfo> {
    ApiResponse::ok(VersionInfo {
        version: CURRENT_MC_VERSION.to_string(),
        map: false,
        mcdr: false,
        monitor_history_enabled: false,
        code_of_conduct: false,
    })
}
