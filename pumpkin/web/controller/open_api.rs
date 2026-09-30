use std::{collections::HashMap, sync::Arc};

use axum::{
    Extension,
    extract::{OriginalUri, Path, Query, Request, State},
    http::StatusCode,
    middleware::Next,
    response::{IntoResponse, Response},
};
use serde::{Deserialize, Serialize};
use serde_json::json;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    storage::{JsonFile, Storage},
    web::response::{ApiError, ApiResponse},
};

const INTERFACE_NAMES: [&str; 5] = ["info", "monitor", "plugins", "players", "logs"];
const CONFIG: JsonFile<OpenApiConfig> = JsonFile::new("open-api.json", OpenApiConfig::default);

#[derive(Default, Deserialize, Serialize)]
struct OpenApiConfig {
    enabled: bool,
    interfaces: Option<HashMap<String, Option<bool>>>,
}

impl OpenApiConfig {
    fn ensure_interfaces(&mut self) {
        let interfaces = self.interfaces.get_or_insert_with(HashMap::new);
        for name in INTERFACE_NAMES {
            interfaces
                .entry(name.to_string())
                .or_insert(Some(true))
                .get_or_insert(true);
        }
    }

    fn interface_enabled(&self, name: &str) -> bool {
        self.interfaces
            .as_ref()
            .and_then(|interfaces| interfaces.get(name))
            .copied()
            .flatten()
            .unwrap_or(true)
    }

    fn check_access(&self, path: &str) -> Result<(), ApiError> {
        if !self.enabled {
            return Err(ApiError::service_unavailable("Open API is not enabled."));
        }
        if let Some(name) = path
            .strip_prefix("/open-api/")
            .and_then(|path| path.split('/').next())
            && INTERFACE_NAMES.contains(&name)
            && !self.interface_enabled(name)
        {
            return Err(ApiError::service_unavailable(format!(
                "Interface '{name}' is not enabled."
            )));
        }
        Ok(())
    }
}

#[derive(Deserialize)]
pub(super) struct ToggleQuery {
    enabled: Option<String>,
}

impl ToggleQuery {
    fn enabled(self) -> Result<bool, ApiError> {
        self.enabled
            .map(|enabled| enabled == "1")
            .ok_or_else(|| ApiError::new(StatusCode::BAD_REQUEST, "Status is missing."))
    }
}

pub(super) async fn get_open_api_enabled(
    State(opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let config = load_config(&opanel.storage()).await?;
    Ok(ApiResponse::ok(json!({"enabled": config.enabled})).into_response())
}

pub(super) async fn toggle_open_api(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<ToggleQuery>,
) -> Result<Response, ApiError> {
    let enabled = query.enabled()?;
    opanel
        .storage()
        .update_json(&CONFIG, |config| {
            config.ensure_interfaces();
            config.enabled = enabled;
        })
        .await
        .map_err(internal_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn get_interface_enabled(
    State(opanel): State<Arc<OPanel>>,
    Path(name): Path<String>,
) -> Result<Response, ApiError> {
    validate_interface(&name)?;
    let config = load_config(&opanel.storage()).await?;
    Ok(ApiResponse::ok(json!({"enabled": config.interface_enabled(&name)})).into_response())
}

pub(super) async fn toggle_interface(
    State(opanel): State<Arc<OPanel>>,
    Path(name): Path<String>,
    Query(query): Query<ToggleQuery>,
) -> Result<Response, ApiError> {
    validate_interface(&name)?;
    let enabled = query.enabled()?;
    opanel
        .storage()
        .update_json(&CONFIG, |config| {
            config.ensure_interfaces();
            config
                .interfaces
                .as_mut()
                .unwrap()
                .insert(name, Some(enabled));
        })
        .await
        .map_err(internal_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

pub(super) async fn authorize(
    Extension(opanel): Extension<Arc<OPanel>>,
    OriginalUri(uri): OriginalUri,
    request: Request,
    next: Next,
) -> Result<Response, ApiError> {
    let config = opanel
        .storage()
        .load_json(&CONFIG)
        .await
        .map_err(internal_error)?
        .value;
    config.check_access(uri.path())?;
    Ok(next.run(request).await)
}

async fn load_config(storage: &Storage) -> Result<OpenApiConfig, ApiError> {
    let mut loaded = storage.load_json(&CONFIG).await.map_err(internal_error)?;
    let incomplete = INTERFACE_NAMES.iter().any(|name| {
        loaded
            .value
            .interfaces
            .as_ref()
            .and_then(|interfaces| interfaces.get(*name))
            .is_none_or(Option::is_none)
    });
    if loaded.needs_persist || incomplete {
        loaded.value.ensure_interfaces();
        storage
            .merge_json(&CONFIG, &loaded.value)
            .await
            .map_err(internal_error)?;
    }
    Ok(loaded.value)
}

fn validate_interface(name: &str) -> Result<(), ApiError> {
    if INTERFACE_NAMES.contains(&name) {
        Ok(())
    } else {
        Err(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Unknown interface name.",
        ))
    }
}

fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn default_and_legacy_configs_are_completed_without_losing_settings() {
        let root =
            crate::utils::file::random_temporary_path(&std::env::temp_dir(), "open-api-test")
                .unwrap();
        let storage = Storage::open(root.clone()).await.unwrap();
        let config = load_config(&storage).await.unwrap();
        assert!(!config.enabled);
        assert_eq!(config.interfaces.unwrap().len(), 5);
        for interfaces in [
            json!(null),
            json!({"logs": false, "info": null, "future": false}),
        ] {
            tokio::fs::write(
                root.join("open-api.json"),
                json!({"enabled": true, "interfaces": interfaces, "futureSetting": 7}).to_string(),
            )
            .await
            .unwrap();
            let config = load_config(&storage).await.unwrap();
            assert!(config.enabled);
            assert!(config.interface_enabled("info"));
            assert_eq!(config.interface_enabled("logs"), interfaces.is_null());
            let saved: serde_json::Value =
                serde_json::from_slice(&tokio::fs::read(root.join("open-api.json")).await.unwrap())
                    .unwrap();
            assert_eq!(saved["futureSetting"], 7);
        }
        tokio::fs::remove_dir_all(root).await.unwrap();
    }

    #[test]
    fn access_checks_apply_to_nested_routes_and_default_missing_interfaces_to_enabled() {
        let mut config = OpenApiConfig::default();
        assert_eq!(
            config
                .check_access("/open-api/info")
                .unwrap_err()
                .into_response()
                .status(),
            StatusCode::SERVICE_UNAVAILABLE
        );
        config.enabled = true;
        assert!(config.check_access("/open-api/info").is_ok());
        config.ensure_interfaces();
        config
            .interfaces
            .as_mut()
            .unwrap()
            .insert("logs".into(), Some(false));
        for path in [
            "/open-api/logs",
            "/open-api/logs/",
            "/open-api/logs/latest.log/download",
        ] {
            assert!(config.check_access(path).is_err());
        }
        assert!(config.check_access("/open-api/info").is_ok());
        assert!(config.check_access("/open-api/logs-other").is_ok());
    }

    #[test]
    fn toggles_follow_java_query_semantics_and_reject_unknown_interfaces() {
        assert!(ToggleQuery { enabled: None }.enabled().is_err());
        for (value, expected) in [("1", true), ("0", false), ("true", false), ("", false)] {
            assert_eq!(
                ToggleQuery {
                    enabled: Some(value.into())
                }
                .enabled()
                .unwrap(),
                expected
            );
        }
        assert!(validate_interface("logs").is_ok());
        assert!(validate_interface("unknown").is_err());
    }
}
