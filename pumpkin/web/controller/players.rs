use std::{
    collections::HashSet,
    path::{Path, PathBuf},
    str::FromStr,
    sync::{Arc, PoisonError, atomic::Ordering},
};

use axum::{
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD};
use pumpkin::{
    data::banlist_serializer::BannedPlayerEntry, entity::player::Player, net::DisconnectReason,
    server::Server,
};
use pumpkin_config::op::Op;
use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};
use pumpkin_util::{GameMode, PermissionLvl, text::TextComponent};
use serde::Deserialize;
use serde_json::{Value, json};
use time::OffsetDateTime;
use uuid::Uuid;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    utils::{base64::decode_string, file::write_json, player_data},
    web::response::{ApiError, ApiResponse},
};

#[derive(Debug, Default, Deserialize)]
pub(super) struct PlayerQuery {
    uuid: Option<String>,
    r: Option<String>,
    gm: Option<String>,
}

struct TargetPlayer {
    uuid: Uuid,
    name: String,
    online: Option<Arc<Player>>,
}

pub(super) async fn get_players_overview(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = &opanel.context().server;
    ApiResponse::ok(json!({"maxPlayerCount": server.max_players(), "whitelist": server.white_list.load(Ordering::Relaxed)})).into_response()
}

pub(super) async fn get_players(State(opanel): State<Arc<OPanel>>) -> Result<Response, ApiError> {
    let server = opanel.context().server.clone();
    let players = tokio::task::spawn_blocking(move || collect_players(&server))
        .await
        .map_err(internal_error)??;
    Ok(ApiResponse::ok(json!({"players": players})).into_response())
}

pub(super) async fn give_op(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, _| {
        set_operator(server, target, true)
    })
    .await
}

pub(super) async fn deprive_op(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, _| {
        set_operator(server, target, false)
    })
    .await
}

pub(super) async fn kick_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |_, target, query| {
        let player = target
            .online
            .as_ref()
            .ok_or_else(|| ApiError::new(StatusCode::FORBIDDEN, "Player is offline."))?;
        let reason = reason(query)?.unwrap_or_else(|| "Kicked by an operator.".into());
        player.kick(DisconnectReason::Kicked, &TextComponent::text(reason));
        Ok(())
    })
    .await
}

pub(super) async fn ban_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, query| {
        let reason = reason(query)?.unwrap_or_else(|| "Banned by an operator.".into());
        let mut bans = server
            .data
            .banned_player_list
            .write()
            .unwrap_or_else(PoisonError::into_inner);
        if bans
            .banned_players
            .iter()
            .any(|entry| entry.uuid == target.uuid && active_ban(entry))
        {
            return Ok(());
        }
        bans.banned_players
            .retain(|entry| entry.uuid != target.uuid);
        bans.banned_players.push(BannedPlayerEntry {
            uuid: target.uuid,
            name: target.name.clone(),
            created: OffsetDateTime::now_utc(),
            source: "OPanel".into(),
            expires: None,
            reason: reason.clone(),
        });
        write_json(Path::new("data/banned-players.json"), &*bans).map_err(internal_error)?;
        drop(bans);
        if let Some(player) = &target.online {
            player.kick(
                DisconnectReason::Kicked,
                &TextComponent::text(format!(
                    "You are banned from this server!\nReason: {reason}"
                )),
            );
        }
        Ok(())
    })
    .await
}

pub(super) async fn pardon_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, _| {
        let mut bans = server
            .data
            .banned_player_list
            .write()
            .unwrap_or_else(PoisonError::into_inner);
        bans.banned_players
            .retain(|entry| entry.uuid != target.uuid);
        write_json(Path::new("data/banned-players.json"), &*bans).map_err(internal_error)
    })
    .await
}

pub(super) async fn set_gamemode(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, query| {
        let mode = parse_game_mode(query.gm.as_deref())?;
        if let Some(player) = &target.online {
            player.set_gamemode(mode);
        } else {
            player_data::set_game_mode(&data_directory(server), target.uuid, mode)
                .map_err(internal_error)?;
        }
        Ok(())
    })
    .await
}

pub(super) async fn delete_player_data(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |server, target, _| {
        player_data::delete(&data_directory(server), target.uuid).map_err(internal_error)
    })
    .await
}

async fn apply(
    opanel: Arc<OPanel>,
    query: PlayerQuery,
    action: impl FnOnce(&Arc<Server>, &TargetPlayer, &PlayerQuery) -> Result<(), ApiError>
    + Send
    + 'static,
) -> Result<Response, ApiError> {
    let uuid = parse_uuid(query.uuid.as_deref())?;
    let server = opanel.context().server.clone();
    tokio::task::spawn_blocking(move || {
        let target = target_player(&server, uuid)?;
        action(&server, &target, &query)
    })
    .await
    .map_err(internal_error)??;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
}

fn target_player(server: &Server, uuid: Uuid) -> Result<TargetPlayer, ApiError> {
    if let Some(player) = server.get_player_by_uuid(uuid) {
        return Ok(TargetPlayer {
            uuid,
            name: player.gameprofile.name.clone(),
            online: Some(player),
        });
    }
    let cached = server
        .data
        .user_cache
        .write()
        .unwrap_or_else(PoisonError::into_inner)
        .get_by_uuid(uuid);
    if let Some(cached) = cached
        && data_directory(server).join(format!("{uuid}.dat")).is_file()
    {
        return Ok(TargetPlayer {
            uuid,
            name: cached.name,
            online: None,
        });
    }
    Err(ApiError::new(StatusCode::NOT_FOUND, "Player not found."))
}

fn collect_players(server: &Server) -> Result<Vec<Value>, ApiError> {
    let online = server.get_all_players();
    let online_ids: HashSet<_> = online.iter().map(|player| player.gameprofile.id).collect();
    let mut players = Vec::new();
    for player in online {
        let target = TargetPlayer {
            uuid: player.gameprofile.id,
            name: player.gameprofile.name.clone(),
            online: Some(player),
        };
        players.push(serialize_player(server, &target)?);
    }
    for uuid in player_data::list(&data_directory(server)).map_err(internal_error)? {
        if online_ids.contains(&uuid) {
            continue;
        }
        if let Ok(target) = target_player(server, uuid) {
            match serialize_player(server, &target) {
                Ok(player) => players.push(player),
                Err(error) => tracing::warn!(?error, %uuid, "Failed to read offline player data"),
            }
        }
    }
    Ok(players)
}

fn serialize_player(server: &Server, target: &TargetPlayer) -> Result<Value, ApiError> {
    let (mode, position) = if let Some(player) = &target.online {
        let position = player.living_entity.entity.pos.load();
        (player.gamemode.load(), [position.x, position.y, position.z])
    } else {
        offline_state(
            &player_data::read(&data_directory(server), target.uuid).map_err(internal_error)?,
        )
    };
    let is_op = server
        .data
        .operator_config
        .read()
        .unwrap_or_else(PoisonError::into_inner)
        .get_entry(&target.uuid)
        .is_some();
    let ban_reason = server
        .data
        .banned_player_list
        .read()
        .unwrap_or_else(PoisonError::into_inner)
        .banned_players
        .iter()
        .find(|entry| entry.uuid == target.uuid && active_ban(entry))
        .map(|entry| entry.reason.clone());
    let mut data = json!({
        "name": target.name, "uuid": target.uuid, "isOnline": target.online.is_some(), "isOp": is_op,
        "isBanned": ban_reason.is_some(), "gamemode": mode.name(),
        "position": {"x": position[0], "y": position[1], "z": position[2]}
    });
    if let Some(reason) = ban_reason {
        data["banReason"] = json!(STANDARD.encode(reason));
    }
    if server.white_list.load(Ordering::Relaxed) {
        data["isWhitelisted"] = json!(
            server
                .data
                .whitelist_config
                .read()
                .unwrap_or_else(PoisonError::into_inner)
                .whitelist
                .iter()
                .any(|entry| entry.name == target.name)
        );
    }
    if let Some(player) = &target.online {
        data["ping"] = json!(player.ping.load(Ordering::Relaxed));
        data["ip"] = json!(player.client.address().ip().to_string());
        data["joinTime"] = Value::Null;
    }
    Ok(data)
}

fn set_operator(
    server: &Arc<Server>,
    target: &TargetPlayer,
    enabled: bool,
) -> Result<(), ApiError> {
    let mut operators = server
        .data
        .operator_config
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    let exists = operators.get_entry(&target.uuid).is_some();
    if exists == enabled {
        return Ok(());
    }
    let level = if enabled {
        server.basic_config.op_permission_level
    } else {
        PermissionLvl::Zero
    };
    if enabled {
        operators
            .ops
            .push(Op::new(target.uuid, target.name.clone(), level, false));
    } else {
        operators.ops.retain(|entry| entry.uuid != target.uuid);
    }
    write_json(Path::new("data/ops.json"), &*operators).map_err(internal_error)?;
    drop(operators);
    if let Some(player) = &target.online {
        player.set_permission_lvl(server, level, &server.command_dispatcher.load());
    }
    Ok(())
}

fn data_directory(server: &Server) -> PathBuf {
    server.basic_config.get_world_path().join("players/data")
}
fn active_ban(entry: &BannedPlayerEntry) -> bool {
    entry
        .expires
        .is_none_or(|expires| expires >= OffsetDateTime::now_utc())
}
fn reason(query: &PlayerQuery) -> Result<Option<String>, ApiError> {
    query
        .r
        .as_deref()
        .map(decode_string)
        .transpose()
        .map_err(|_| bad_request("Invalid Base64 encoding in reason."))
}
fn parse_uuid(value: Option<&str>) -> Result<Uuid, ApiError> {
    Uuid::parse_str(value.ok_or_else(|| bad_request("Uuid is required."))?)
        .map_err(|_| bad_request("Invalid UUID."))
}
fn parse_game_mode(value: Option<&str>) -> Result<GameMode, ApiError> {
    GameMode::from_str(value.ok_or_else(|| bad_request("Gamemode is required."))?)
        .map_err(|_| bad_request("Invalid gamemode."))
}
fn bad_request(message: &str) -> ApiError {
    ApiError::new(StatusCode::BAD_REQUEST, message)
}
fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

fn offline_state(data: &NbtCompound) -> (GameMode, [f64; 3]) {
    let mode = match data.get_int("playerGameType") {
        Some(1) => GameMode::Creative,
        Some(2) => GameMode::Adventure,
        Some(3) => GameMode::Spectator,
        _ => GameMode::Survival,
    };
    let mut position = [0.0; 3];
    if let Some(values) = data.get_list("Pos") {
        for (output, value) in position.iter_mut().zip(values) {
            if let NbtTag::Double(value) = value {
                *output = *value;
            }
        }
    }
    (mode, position)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn missing_or_invalid_targets_and_modes_are_bad_requests() {
        for uuid in [None, Some("../../secret"), Some("invalid")] {
            assert_eq!(
                parse_uuid(uuid).unwrap_err().into_response().status(),
                StatusCode::BAD_REQUEST
            );
        }
        assert_eq!(
            parse_uuid(Some("00000000-0000-0000-0000-000000000001")).unwrap(),
            Uuid::from_u128(1)
        );
        assert!(parse_game_mode(None).is_err());
        assert!(parse_game_mode(Some("invalid")).is_err());
        assert_eq!(
            parse_game_mode(Some("creative")).unwrap(),
            GameMode::Creative
        );
        assert!(
            reason(&PlayerQuery {
                r: Some("invalid!".into()),
                ..Default::default()
            })
            .is_err()
        );
    }

    #[test]
    fn offline_state_reads_position_and_mode_with_defaults() {
        let mut data = NbtCompound::new();
        assert_eq!(offline_state(&data), (GameMode::Survival, [0.0; 3]));
        data.put_int("playerGameType", 3);
        data.put(
            "Pos",
            NbtTag::List(vec![
                NbtTag::Double(1.25),
                NbtTag::Double(64.0),
                NbtTag::Double(-5.5),
            ]),
        );
        assert_eq!(
            offline_state(&data),
            (GameMode::Spectator, [1.25, 64.0, -5.5])
        );
    }
}
