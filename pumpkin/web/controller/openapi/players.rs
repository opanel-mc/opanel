use std::sync::Arc;

use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde_json::{Value, json};
use uuid::Uuid;

use crate::{
    opanel::OPanel,
    player::{Player, PlayerError},
    utils::base64::decode_string,
    web::response::{ApiError, ApiResponse},
};

pub(in crate::web::controller) async fn get_players(
    State(opanel): State<Arc<OPanel>>,
) -> Result<Response, ApiError> {
    let server = opanel.context().server.clone();
    let players = tokio::task::spawn_blocking(move || {
        Player::list(server)
            .map_err(player_error)?
            .into_iter()
            .map(public_player)
            .collect::<Result<Vec<_>, _>>()
    })
    .await
    .map_err(internal_error)??;
    Ok(ApiResponse::ok(json!({"players": players})).into_response())
}

pub(in crate::web::controller) async fn get_player_info(
    State(opanel): State<Arc<OPanel>>,
    Path(uuid): Path<String>,
) -> Result<Response, ApiError> {
    let uuid = Uuid::parse_str(&uuid)
        .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "Invalid UUID."))?;
    let server = opanel.context().server.clone();
    let player = tokio::task::spawn_blocking(move || {
        let player = Player::find(server, uuid).map_err(player_error)?;
        public_player(player.snapshot().map_err(player_error)?)
    })
    .await
    .map_err(internal_error)??;
    Ok(ApiResponse::ok(player).into_response())
}

// Use an allowlist so future fields added to panel snapshots stay private.
fn public_player(mut snapshot: Value) -> Result<Value, ApiError> {
    let mut public = json!({});
    for key in ["name", "uuid", "isOnline", "isBanned", "gamemode"] {
        public[key] = snapshot[key].take();
    }
    if public["isBanned"] == true
        && let Some(reason) = snapshot["banReason"].as_str()
    {
        public["banReason"] = json!(decode_string(reason).map_err(internal_error)?);
    }
    if public["isOnline"] == true {
        public["ping"] = snapshot["ping"].take();
    }
    Ok(public)
}

fn player_error(error: PlayerError) -> ApiError {
    match error {
        PlayerError::NotFound => ApiError::new(StatusCode::NOT_FOUND, "Cannot find the player."),
        error => super::super::players::player_error(error),
    }
}

fn internal_error(error: impl std::fmt::Display) -> ApiError {
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use base64::{Engine as _, engine::general_purpose::STANDARD};

    #[test]
    fn projection_excludes_private_fields_and_decodes_ban_reasons() {
        let public = public_player(json!({
            "name": "Steve", "uuid": Uuid::nil(), "isOnline": true, "isBanned": true,
            "gamemode": "survival", "banReason": STANDARD.encode("禁止进入"), "ping": 42,
            "ip": "127.0.0.1", "position": {"x": 1}, "isOp": true, "isWhitelisted": true,
            "joinTime": 100, "futurePrivateField": "secret"
        }))
        .unwrap();
        assert_eq!(
            public,
            json!({
                "name": "Steve", "uuid": Uuid::nil(), "isOnline": true, "isBanned": true,
                "gamemode": "survival", "banReason": "禁止进入", "ping": 42
            })
        );
    }

    #[test]
    fn offline_unbanned_players_omit_conditional_fields() {
        let public = public_player(json!({
            "name": "Alex", "uuid": Uuid::nil(), "isOnline": false, "isBanned": false,
            "gamemode": "creative", "banReason": "ignored", "ping": 42
        }))
        .unwrap();
        assert!(public.get("banReason").is_none());
        assert!(public.get("ping").is_none());
        assert_eq!(
            player_error(PlayerError::NotFound).into_response().status(),
            StatusCode::NOT_FOUND
        );
    }
}
