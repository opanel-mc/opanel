use std::sync::{Arc, atomic::Ordering};

use axum::{
    extract::State,
    response::{IntoResponse, Response},
};
use serde::Serialize;

use crate::{
    opanel::OPanel,
    utils::{
        system::{SystemInfo, collect_system_info},
        time::IngameTime,
    },
    web::response::ApiResponse,
};

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ServerInfo {
    motd: String,
    port: u16,
    max_player_count: u32,
    whitelist: bool,
    uptime: u64,
    ingame_time: i64,
    system: SystemInfo,
}

pub(in crate::web::controller) async fn get_server_info(
    State(opanel): State<Arc<OPanel>>,
) -> Response {
    let context = opanel.context();
    let server = &context.server;
    let motd = server
        .get_status()
        .lock()
        .unwrap_or_else(std::sync::PoisonError::into_inner)
        .status_response
        .description
        .clone()
        .get_text();

    ApiResponse::ok(ServerInfo {
        motd,
        port: server.advanced_config.networking.java.address.port(),
        max_player_count: server.max_players(),
        whitelist: server.white_list.load(Ordering::Relaxed),
        uptime: opanel.uptimer().current(),
        ingame_time: IngameTime::from_server(server).current,
        system: collect_system_info(),
    })
    .into_response()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn public_info_keeps_plain_motd_and_numeric_time_without_panel_fields() {
        let value = serde_json::to_value(ApiResponse::ok(ServerInfo {
            motd: "§a欢迎\nSecond line".into(),
            port: 25565,
            max_player_count: 20,
            whitelist: false,
            uptime: 42,
            ingame_time: 6000,
            system: collect_system_info(),
        }))
        .unwrap();
        assert_eq!(value["motd"], "§a欢迎\nSecond line");
        assert_eq!(value["ingameTime"], 6000);
        assert_eq!(value["maxPlayerCount"], 20);
        assert_eq!(value["code"], 200);
        assert!(value.get("favicon").is_none());
        assert!(value.get("realtimeMotd").is_none());
        assert_eq!(value["system"]["java"], "N/A");
    }
}
