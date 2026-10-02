use std::{
    str::FromStr,
    sync::{Arc, atomic::Ordering},
};

use axum::{
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use pumpkin_util::GameMode;
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use super::control::EmptyPayload;
use crate::{
    opanel::OPanel,
    player::{Player, PlayerError},
    utils::base64::decode_string,
    web::response::{ApiError, ApiResponse},
};

#[derive(Debug, Default, Deserialize)]
pub(super) struct PlayerQuery {
    uuid: Option<String>,
    r: Option<String>,
    gm: Option<String>,
}

pub(super) async fn get_players_overview(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = &opanel.context().server;
    ApiResponse::ok(json!({"maxPlayerCount": server.max_players(), "whitelist": server.white_list.load(Ordering::Relaxed)})).into_response()
}

pub(super) async fn get_players(State(opanel): State<Arc<OPanel>>) -> Result<Response, ApiError> {
    let server = opanel.context().server.clone();
    let players = tokio::task::spawn_blocking(move || Player::list(server))
        .await
        .map_err(internal_error)?
        .map_err(player_error)?;
    Ok(ApiResponse::ok(json!({"players": players})).into_response())
}

pub(super) async fn give_op(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, _| {
        player.set_operator(true);
        Ok(())
    })
    .await
}

pub(super) async fn deprive_op(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, _| {
        player.set_operator(false);
        Ok(())
    })
    .await
}

pub(super) async fn kick_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, query| {
        if !player.is_online() {
            return Err(player_error(PlayerError::Offline));
        }
        player.kick(reason(query)?).map_err(player_error)
    })
    .await
}

pub(super) async fn ban_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, query| {
        player.ban(reason(query)?);
        Ok(())
    })
    .await
}

pub(super) async fn pardon_player(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, _| {
        player.pardon();
        Ok(())
    })
    .await
}

pub(super) async fn set_gamemode(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, query| {
        let mode = parse_game_mode(query.gm.as_deref())?;
        player.set_game_mode(mode).map_err(player_error)
    })
    .await
}

pub(super) async fn delete_player_data(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<PlayerQuery>,
) -> Result<Response, ApiError> {
    apply(opanel, query, |player, _| {
        player.delete_data().map_err(player_error)
    })
    .await
}

async fn apply(
    opanel: Arc<OPanel>,
    query: PlayerQuery,
    action: impl FnOnce(&Player, &PlayerQuery) -> Result<(), ApiError> + Send + 'static,
) -> Result<Response, ApiError> {
    let uuid = parse_uuid(query.uuid.as_deref())?;
    let server = opanel.context().server.clone();
    tokio::task::spawn_blocking(move || {
        let player = Player::find(server, uuid).map_err(player_error)?;
        action(&player, &query)
    })
    .await
    .map_err(internal_error)??;
    Ok(ApiResponse::ok(EmptyPayload {}).into_response())
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

pub(super) fn player_error(error: PlayerError) -> ApiError {
    let status = match &error {
        PlayerError::NotFound => StatusCode::NOT_FOUND,
        PlayerError::Offline => StatusCode::FORBIDDEN,
        PlayerError::Io(_) => StatusCode::INTERNAL_SERVER_ERROR,
    };
    ApiError::new(status, error.to_string())
}

fn bad_request(message: &str) -> ApiError {
    ApiError::new(StatusCode::BAD_REQUEST, message)
}

fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
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

    #[tokio::test]
    async fn player_errors_preserve_http_status_and_message() {
        for (error, status, message) in [
            (
                PlayerError::NotFound,
                StatusCode::NOT_FOUND,
                "Player not found.",
            ),
            (
                PlayerError::Offline,
                StatusCode::FORBIDDEN,
                "Player is offline.",
            ),
            (
                PlayerError::Io(std::io::Error::other("failed to read player data")),
                StatusCode::INTERNAL_SERVER_ERROR,
                "failed to read player data",
            ),
        ] {
            let response = player_error(error).into_response();
            assert_eq!(response.status(), status);
            let body = axum::body::to_bytes(response.into_body(), usize::MAX)
                .await
                .unwrap();
            assert_eq!(
                serde_json::from_slice::<serde_json::Value>(&body).unwrap(),
                json!({"code": status.as_u16(), "error": message})
            );
        }
    }
}
