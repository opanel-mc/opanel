use std::{
    io,
    sync::{Arc, atomic::Ordering},
};

use axum::{
    body::Bytes,
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{
    Engine as _, alphabet,
    engine::general_purpose::GeneralPurposeConfig,
    engine::{DecodePaddingMode, general_purpose::GeneralPurpose},
};
use pumpkin_util::text::TextComponent;
use serde::Serialize;
use sysinfo::{CpuRefreshKind, MemoryRefreshKind, RefreshKind, System};
use thiserror::Error;
use tokio::fs;
use toml_edit::{DocumentMut, Item, Table, value};
use tracing::error;

use crate::{
    opanel::OPanel,
    utils::time::{IngameTime, unix_time_millis},
    web::response::{ApiError, ApiResponse},
};

const PUMPKIN_CONFIG_PATH: &str = "pumpkin.toml";

const BASE64_ENGINE: GeneralPurpose = GeneralPurpose::new(
    &alphabet::STANDARD,
    GeneralPurposeConfig::new().with_decode_padding_mode(DecodePaddingMode::Indifferent),
);

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ServerInfo {
    favicon: Option<String>,
    motd: String,
    port: u16,
    max_player_count: u32,
    whitelist: bool,
    uptime: u64,
    ingame_time: IngameTime,
    system: SystemInfo,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct SystemInfo {
    os: String,
    arch: &'static str,
    cpu_name: String,
    cpu_core: usize,
    cpu_thread: usize,
    memory: u64,
    jvm_memory: u64,
    gpus: Vec<String>,
    java: &'static str,
}

#[derive(Debug, Serialize)]
struct EmptyPayload {}

#[derive(Debug, Error)]
enum PersistMotdError {
    #[error("failed to read {PUMPKIN_CONFIG_PATH}: {0}")]
    Read(#[source] io::Error),
    #[error("invalid {PUMPKIN_CONFIG_PATH}: {0}")]
    InvalidConfig(#[source] UpdateMotdDocumentError),
    #[error("failed to write {PUMPKIN_CONFIG_PATH}: {0}")]
    Write(#[source] io::Error),
}

#[derive(Debug, Error)]
enum UpdateMotdDocumentError {
    #[error(transparent)]
    Parse(#[from] toml_edit::TomlError),
    #[error("`{path}` must be a table")]
    ExpectedTable { path: &'static str },
    #[error("`{path}` must be a value")]
    ExpectedValue { path: &'static str },
}

pub(super) async fn get_server_info(State(opanel): State<Arc<OPanel>>) -> Response {
    let context = opanel.context();
    let server = &context.server;

    let (motd, has_favicon) = {
        let status = server
            .get_status()
            .lock()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        (
            status.status_response.description.clone().get_text(),
            status.status_response.favicon.is_some(),
        )
    };

    let favicon = has_favicon.then(|| format!("/api/icon?t={}", unix_time_millis()));

    ApiResponse::ok(ServerInfo {
        favicon,
        motd: BASE64_ENGINE.encode(motd),
        port: server.advanced_config.networking.java.address.port(),
        max_player_count: server.max_players(),
        whitelist: server.white_list.load(Ordering::Relaxed),
        uptime: opanel.uptimer().current(),
        ingame_time: IngameTime::from_server(server),
        system: collect_system_info(),
    })
    .into_response()
}

pub(super) async fn set_motd(State(opanel): State<Arc<OPanel>>, body: Bytes) -> Response {
    if is_missing_motd(&body) {
        return ApiError::new(StatusCode::BAD_REQUEST, "Motd is missing.").into_response();
    }

    let decoded = match BASE64_ENGINE.decode(&body) {
        Ok(decoded) => decoded,
        Err(error) => {
            error!(%error, "invalid Base64 encoding in motd");
            return ApiError::new(StatusCode::BAD_REQUEST, "Invalid Base64 encoding in motd.")
                .into_response();
        }
    };
    let motd = String::from_utf8_lossy(&decoded).into_owned();

    {
        let context = opanel.context();
        let mut status = context
            .server
            .get_status()
            .lock()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        status.status_response.description = TextComponent::text(motd.clone());
    }

    if let Err(error) = persist_motd(&motd).await {
        error!(%error, "failed to update motd");
        return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response();
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

fn is_missing_motd(body: &[u8]) -> bool {
    body.is_empty() || body.iter().all(|byte| *byte <= b' ')
}

fn collect_system_info() -> SystemInfo {
    if !sysinfo::IS_SUPPORTED_SYSTEM {
        return SystemInfo {
            os: std::env::consts::OS.to_string(),
            arch: std::env::consts::ARCH,
            cpu_name: "Unknown".to_string(),
            cpu_core: 0,
            cpu_thread: 0,
            memory: 0,
            jvm_memory: 0,
            gpus: Vec::new(),
            java: "N/A",
        };
    }

    let system = System::new_with_specifics(
        RefreshKind::nothing()
            .with_cpu(CpuRefreshKind::nothing())
            .with_memory(MemoryRefreshKind::nothing().with_ram()),
    );
    let cpu_thread = system.cpus().len();
    let cpu_core = System::physical_core_count().unwrap_or(cpu_thread);
    let cpu_name = system.cpus().first().map_or_else(
        || "Unknown".to_string(),
        |cpu| {
            let brand = cpu.brand().trim();
            if brand.is_empty() {
                "Unknown".to_string()
            } else {
                brand.to_string()
            }
        },
    );
    let memory = system.total_memory();

    SystemInfo {
        os: System::long_os_version()
            .or_else(System::name)
            .unwrap_or_else(|| std::env::consts::OS.to_string()),
        arch: std::env::consts::ARCH,
        cpu_name,
        cpu_core,
        cpu_thread,
        memory,
        // Native Pumpkin has no JVM heap limit. Total physical memory is the practical upper
        // bound used by the existing frontend's runtime-memory display.
        jvm_memory: memory,
        // sysinfo deliberately does not expose cross-platform GPU enumeration.
        gpus: Vec::new(),
        java: "N/A",
    }
}

async fn persist_motd(motd: &str) -> Result<(), PersistMotdError> {
    let contents = fs::read_to_string(PUMPKIN_CONFIG_PATH)
        .await
        .map_err(PersistMotdError::Read)?;
    let updated = update_motd_document(&contents, motd).map_err(PersistMotdError::InvalidConfig)?;
    fs::write(PUMPKIN_CONFIG_PATH, updated)
        .await
        .map_err(PersistMotdError::Write)
}

fn update_motd_document(contents: &str, motd: &str) -> Result<String, UpdateMotdDocumentError> {
    let mut document = contents.parse::<DocumentMut>()?;
    let networking_item = document
        .as_table_mut()
        .entry("networking")
        .or_insert_with(|| Item::Table(Table::new()));
    let networking = networking_item
        .as_table_like_mut()
        .ok_or(UpdateMotdDocumentError::ExpectedTable { path: "networking" })?;
    let java_item = networking
        .entry("java")
        .or_insert_with(|| Item::Table(Table::new()));
    let java = java_item
        .as_table_like_mut()
        .ok_or(UpdateMotdDocumentError::ExpectedTable {
            path: "networking.java",
        })?;
    let motd_item = java.entry("motd").or_insert_with(|| value(motd));
    if !motd_item.is_value() {
        return Err(UpdateMotdDocumentError::ExpectedValue {
            path: "networking.java.motd",
        });
    }
    let decor = motd_item
        .as_value()
        .map(|existing_value| existing_value.decor().clone());
    *motd_item = value(motd);
    if let (Some(decor), Some(updated_value)) = (decor, motd_item.as_value_mut()) {
        *updated_value.decor_mut() = decor;
    }
    Ok(document.to_string())
}

#[cfg(test)]
mod tests {
    use base64::Engine as _;
    use serde_json::json;

    use super::{
        BASE64_ENGINE, IngameTime, ServerInfo, SystemInfo, is_missing_motd, update_motd_document,
    };

    #[test]
    fn base64_decoder_accepts_java_compatible_padding() {
        assert_eq!(BASE64_ENGINE.decode("SGVsbG8=").unwrap(), b"Hello");
        assert_eq!(BASE64_ENGINE.decode("SGVsbG8").unwrap(), b"Hello");
        assert!(BASE64_ENGINE.decode("SGVs bG8=").is_err());
    }

    #[test]
    fn missing_motd_matches_java_trim_semantics() {
        assert!(is_missing_motd(b""));
        assert!(is_missing_motd(b" \t\r\n"));
        assert!(is_missing_motd(b"\0\x1f"));
        assert!(!is_missing_motd(b"SGVsbG8="));
    }

    #[test]
    fn server_info_uses_the_frontend_field_names() {
        let value = serde_json::to_value(ServerInfo {
            favicon: Some("/api/icon?t=1".to_string()),
            motd: "SGVsbG8=".to_string(),
            port: 25_565,
            max_player_count: 20,
            whitelist: true,
            uptime: 42,
            ingame_time: IngameTime {
                current: 6_000,
                do_daylight_cycle: true,
                paused: false,
                mspt: 50.0,
            },
            system: SystemInfo {
                os: "Test OS".to_string(),
                arch: "test-arch",
                cpu_name: "Test CPU".to_string(),
                cpu_core: 4,
                cpu_thread: 8,
                memory: 16,
                jvm_memory: 16,
                gpus: vec!["Test GPU".to_string()],
                java: "N/A",
            },
        })
        .unwrap();

        assert_eq!(
            value,
            json!({
                "favicon": "/api/icon?t=1",
                "motd": "SGVsbG8=",
                "port": 25565,
                "maxPlayerCount": 20,
                "whitelist": true,
                "uptime": 42,
                "ingameTime": {
                    "current": 6000,
                    "doDaylightCycle": true,
                    "paused": false,
                    "mspt": 50.0
                },
                "system": {
                    "os": "Test OS",
                    "arch": "test-arch",
                    "cpuName": "Test CPU",
                    "cpuCore": 4,
                    "cpuThread": 8,
                    "memory": 16,
                    "jvmMemory": 16,
                    "gpus": ["Test GPU"],
                    "java": "N/A"
                }
            })
        );
    }

    #[test]
    fn motd_update_preserves_the_rest_of_pumpkin_config() {
        let input = r#"# server configuration
[networking.java]
address = "0.0.0.0:25565"
motd = "Old MOTD" # keep this setting here

[plugins]
enabled = true
"#;

        let updated = update_motd_document(input, "New \"MOTD\"\nSecond line").unwrap();
        let document = updated.parse::<toml_edit::DocumentMut>().unwrap();

        assert_eq!(
            document["networking"]["java"]["motd"].as_str(),
            Some("New \"MOTD\"\nSecond line")
        );
        assert_eq!(
            document["networking"]["java"]["address"].as_str(),
            Some("0.0.0.0:25565")
        );
        assert_eq!(document["plugins"]["enabled"].as_bool(), Some(true));
        assert!(updated.contains("# server configuration"));
        assert!(updated.contains("# keep this setting here"));
    }

    #[test]
    fn motd_update_creates_missing_tables_without_panicking() {
        let updated = update_motd_document("# empty configuration\n", "New MOTD").unwrap();
        let document = updated.parse::<toml_edit::DocumentMut>().unwrap();

        assert_eq!(
            document["networking"]["java"]["motd"].as_str(),
            Some("New MOTD")
        );
    }

    #[test]
    fn motd_update_rejects_a_non_table_parent_without_panicking() {
        let error = update_motd_document("networking = \"invalid\"\n", "New MOTD").unwrap_err();

        assert_eq!(error.to_string(), "`networking` must be a table");
    }
}
