use std::sync::Arc;

use axum::{
    extract::State,
    response::{IntoResponse, Response},
};
use pumpkin::plugin::PluginMetadata;
use serde::Serialize;
use serde_json::json;

use crate::{
    opanel::OPanel,
    web::response::{ApiError, ApiResponse},
};

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PluginInfo {
    file_name: Option<String>,
    name: String,
    version: String,
    description: String,
    authors: Vec<String>,
    website: Option<String>,
    icon: Option<String>,
    size: Option<u64>,
    enabled: bool,
    loaded: bool,
}

impl PluginInfo {
    fn from_metadata(metadata: PluginMetadata, active: bool) -> Self {
        Self {
            // Pumpkin only exposes loaded metadata; paths, sizes and icons are private.
            file_name: None,
            name: metadata.name,
            version: metadata.version,
            description: metadata.description,
            authors: metadata.authors,
            website: None,
            icon: None,
            size: None,
            enabled: active,
            loaded: true,
        }
    }
}

pub(in crate::web::controller) async fn get_plugins(State(opanel): State<Arc<OPanel>>) -> Response {
    let context = opanel.context();
    let manager = &context.server.plugin_manager;
    let plugins: Vec<_> = manager
        .loaded_plugins()
        .into_iter()
        .map(|metadata| {
            let active = manager.is_plugin_active(&metadata.name);
            PluginInfo::from_metadata(metadata, active)
        })
        .collect();
    ApiResponse::ok(json!({"plugins": plugins})).into_response()
}

pub(in crate::web::controller) async fn get_plugin_icon(
    State(_opanel): State<Arc<OPanel>>,
) -> ApiError {
    ApiError::service_unavailable("Pumpkin does not expose plugin icons.")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn metadata_preserves_native_plugin_state_and_marks_unavailable_fields() {
        for active in [true, false] {
            let metadata = PluginMetadata {
                name: "Example".into(),
                version: "1.2".into(),
                authors: vec!["Author".into()],
                description: "说明".into(),
                dependencies: Vec::new(),
                permissions: Vec::new(),
            };
            let value = serde_json::to_value(PluginInfo::from_metadata(metadata, active)).unwrap();
            assert_eq!(
                value,
                json!({
                    "fileName": null, "name": "Example", "version": "1.2", "authors": ["Author"],
                    "description": "说明", "website": null, "icon": null, "size": null,
                    "enabled": active, "loaded": true,
                })
            );
        }
    }
}
