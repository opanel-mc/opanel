use std::{
    collections::HashSet,
    io,
    path::PathBuf,
    sync::{Arc, PoisonError, atomic::Ordering},
};

use base64::{Engine as _, engine::general_purpose::STANDARD};
use pumpkin::{
    data::{SaveJSONConfiguration, banlist_serializer::BannedPlayerEntry},
    entity::player::Player as PumpkinPlayer,
    net::DisconnectReason,
    server::Server,
};
use pumpkin_config::op::Op;
use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};
use pumpkin_util::{GameMode, PermissionLvl, text::TextComponent};
use serde_json::{Value, json};
use thiserror::Error;
use time::OffsetDateTime;
use uuid::Uuid;

use crate::utils::player_data;

#[derive(Debug, Error)]
pub(crate) enum PlayerError {
    #[error("Player not found.")]
    NotFound,
    #[error("Player is offline.")]
    Offline,
    #[error(transparent)]
    Io(#[from] io::Error),
}

pub(crate) struct Player {
    server: Arc<Server>,
    uuid: Uuid,
    name: String,
    online: Option<Arc<PumpkinPlayer>>,
}

impl Player {
    pub(crate) fn from_online(server: Arc<Server>, player: Arc<PumpkinPlayer>) -> Self {
        Self {
            server,
            uuid: player.gameprofile.id,
            name: player.gameprofile.name.clone(),
            online: Some(player),
        }
    }

    pub(crate) fn find(server: Arc<Server>, uuid: Uuid) -> Result<Self, PlayerError> {
        if let Some(player) = server.get_player_by_uuid(uuid) {
            return Ok(Self::from_online(server, player));
        }
        let cached = server
            .data
            .user_cache
            .write()
            .unwrap_or_else(PoisonError::into_inner)
            .get_by_uuid(uuid);
        if let Some(cached) = cached
            && data_directory(&server)
                .join(format!("{uuid}.dat"))
                .is_file()
        {
            return Ok(Self {
                server,
                uuid,
                name: cached.name,
                online: None,
            });
        }
        Err(PlayerError::NotFound)
    }

    /// Reads offline player files; call from a blocking task.
    pub(crate) fn list(server: Arc<Server>) -> Result<Vec<Value>, PlayerError> {
        let online = server.get_all_players();
        let online_ids: HashSet<_> = online.iter().map(|player| player.gameprofile.id).collect();
        let mut players = Vec::new();
        for player in online {
            let target = Self::from_online(Arc::clone(&server), player);
            players.push(target.snapshot()?);
        }
        for uuid in player_data::list(&data_directory(&server))? {
            if online_ids.contains(&uuid) {
                continue;
            }
            if let Ok(target) = Self::find(Arc::clone(&server), uuid) {
                match target.snapshot() {
                    Ok(player) => players.push(player),
                    Err(error) => {
                        tracing::warn!(?error, %uuid, "Failed to read offline player data")
                    }
                }
            }
        }
        Ok(players)
    }

    pub(crate) fn is_online(&self) -> bool {
        self.online.is_some()
    }

    pub(crate) fn snapshot(&self) -> Result<Value, PlayerError> {
        let (mode, position) = if let Some(player) = &self.online {
            let position = player.living_entity.entity.pos.load();
            (player.gamemode.load(), [position.x, position.y, position.z])
        } else {
            offline_state(&player_data::read(
                &data_directory(&self.server),
                self.uuid,
            )?)
        };
        let is_op = self
            .server
            .data
            .operator_config
            .read()
            .unwrap_or_else(PoisonError::into_inner)
            .get_entry(&self.uuid)
            .is_some();
        let ban_reason = self
            .server
            .data
            .banned_player_list
            .read()
            .unwrap_or_else(PoisonError::into_inner)
            .banned_players
            .iter()
            .find(|entry| entry.uuid == self.uuid && active_ban(entry))
            .map(|entry| entry.reason.clone());
        let mut data = json!({
            "name": self.name, "uuid": self.uuid, "isOnline": self.is_online(), "isOp": is_op,
            "isBanned": ban_reason.is_some(), "gamemode": mode.name(),
            "position": {"x": position[0], "y": position[1], "z": position[2]}
        });
        if let Some(reason) = ban_reason {
            data["banReason"] = json!(STANDARD.encode(reason));
        }
        if self.server.white_list.load(Ordering::Relaxed) {
            data["isWhitelisted"] = json!(
                self.server
                    .data
                    .whitelist_config
                    .read()
                    .unwrap_or_else(PoisonError::into_inner)
                    .whitelist
                    .iter()
                    .any(|entry| entry.name == self.name)
            );
        }
        if let Some(player) = &self.online {
            data["ping"] = json!(player.ping.load(Ordering::Relaxed));
            data["ip"] = json!(player.client.address().ip().to_string());
            data["joinTime"] = Value::Null;
        }
        Ok(data)
    }

    pub(crate) fn set_operator(&self, enabled: bool) {
        let mut operators = self
            .server
            .data
            .operator_config
            .write()
            .unwrap_or_else(PoisonError::into_inner);
        let exists = operators.get_entry(&self.uuid).is_some();
        if exists == enabled {
            return;
        }
        let level = if enabled {
            self.server.basic_config.op_permission_level
        } else {
            PermissionLvl::Zero
        };
        if enabled {
            operators
                .ops
                .push(Op::new(self.uuid, self.name.clone(), level, false));
        } else {
            operators.ops.retain(|entry| entry.uuid != self.uuid);
        }
        operators.save();
        drop(operators);
        if let Some(player) = &self.online {
            player.set_permission_lvl(&self.server, level, &self.server.command_dispatcher.load());
        }
    }

    pub(crate) fn kick(&self, reason: Option<String>) -> Result<(), PlayerError> {
        let player = self.online.as_ref().ok_or(PlayerError::Offline)?;
        let reason = reason.unwrap_or_else(|| "Kicked by an operator.".into());
        player.kick(DisconnectReason::Kicked, &TextComponent::text(reason));
        Ok(())
    }

    pub(crate) fn ban(&self, reason: Option<String>) {
        if let Some(player) = &self.online {
            player.ban_explicit(
                &self.server,
                reason.map(TextComponent::text),
                Some("OPanel".into()),
                None,
                true,
                true,
            );
            return;
        }

        let mut bans = self
            .server
            .data
            .banned_player_list
            .write()
            .unwrap_or_else(PoisonError::into_inner);
        if bans
            .banned_players
            .iter()
            .any(|entry| entry.uuid == self.uuid && active_ban(entry))
        {
            return;
        }
        bans.banned_players.retain(|entry| entry.uuid != self.uuid);
        bans.banned_players.push(BannedPlayerEntry {
            uuid: self.uuid,
            name: self.name.clone(),
            created: OffsetDateTime::now_utc(),
            source: "OPanel".into(),
            expires: None,
            reason: reason.unwrap_or_else(|| "Banned by an operator.".into()),
        });
        bans.save();
    }

    pub(crate) fn pardon(&self) {
        let mut bans = self
            .server
            .data
            .banned_player_list
            .write()
            .unwrap_or_else(PoisonError::into_inner);
        bans.banned_players.retain(|entry| entry.uuid != self.uuid);
        bans.save();
    }

    pub(crate) fn set_game_mode(&self, mode: GameMode) -> Result<(), PlayerError> {
        if let Some(player) = &self.online {
            player.set_gamemode(mode);
        } else {
            player_data::set_game_mode(&data_directory(&self.server), self.uuid, mode)?;
        }
        Ok(())
    }

    pub(crate) fn delete_data(&self) -> Result<(), PlayerError> {
        player_data::delete(&data_directory(&self.server), self.uuid)?;
        Ok(())
    }
}

fn data_directory(server: &Server) -> PathBuf {
    server.basic_config.get_world_path().join("players/data")
}

fn active_ban(entry: &BannedPlayerEntry) -> bool {
    entry
        .expires
        .is_none_or(|expires| expires >= OffsetDateTime::now_utc())
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
