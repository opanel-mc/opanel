use std::{collections::BTreeMap, sync::Arc};

use axum::{
    body::Bytes,
    extract::{Path, Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use pumpkin::server::Server;
use pumpkin_data::game_rules::{GameRule, GameRuleRegistry, GameRuleValue};
use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::{
    opanel::OPanel,
    web::{
        controller::control::EmptyPayload,
        response::{ApiError, ApiResponse},
    },
};

#[derive(Debug, Serialize)]
struct GamerulesPayload {
    gamerules: BTreeMap<String, Value>,
}

#[derive(Debug, Deserialize)]
struct GamerulesEditRequest {
    gamerules: Option<BTreeMap<String, Value>>,
}

#[derive(Debug, Deserialize)]
pub(super) struct GameruleQuery {
    key: Option<String>,
    value: Option<String>,
}

pub(super) async fn get_gamerules(
    State(opanel): State<Arc<OPanel>>,
    Path(dimension): Path<String>,
) -> Response {
    if !is_dimension_name(&dimension) {
        return bad_request("Illegal dimension name.");
    }

    let level_info = opanel.context().server.level_info.load();
    ApiResponse::ok(GamerulesPayload {
        gamerules: gamerules_payload(&level_info.game_rules),
    })
    .into_response()
}

pub(super) async fn change_gamerule(
    State(opanel): State<Arc<OPanel>>,
    Path(dimension): Path<String>,
    body: Bytes,
) -> Response {
    if !is_dimension_name(&dimension) {
        return bad_request("Illegal dimension name.");
    }
    let request: GamerulesEditRequest = match serde_json::from_slice(&body) {
        Ok(request) => request,
        Err(error) => return bad_request(error.to_string()),
    };
    let Some(changes) = request.gamerules else {
        return bad_request("Gamerules is missing.");
    };

    let server = &opanel.context().server;
    let current_info = server.level_info.load();
    let mut updated = (**current_info).clone();
    let spectators_changed = match apply_gamerules(&mut updated.game_rules, &changes) {
        Ok(changed) => changed,
        Err(error) => return bad_request(error),
    };
    drop(current_info);
    server.level_info.store(Arc::new(updated));
    apply_gamerule_side_effects(server, spectators_changed);

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn patch_gamerule(
    State(opanel): State<Arc<OPanel>>,
    Path(dimension): Path<String>,
    Query(query): Query<GameruleQuery>,
) -> Response {
    if !is_dimension_name(&dimension) {
        return bad_request("Illegal dimension name.");
    }
    let Some(key) = query.key else {
        return bad_request("Key is missing.");
    };
    let Some(raw_value) = query.value else {
        return bad_request("Value is missing.");
    };
    let Some(rule) = find_gamerule(&key) else {
        return ApiError::new(StatusCode::NOT_FOUND, "Cannot find the specified gamerule.")
            .into_response();
    };

    let server = &opanel.context().server;
    let current_info = server.level_info.load();
    let value = match parse_query_value(&current_info.game_rules, &rule, &raw_value) {
        Ok(value) => value,
        Err(error) => return bad_request(error),
    };
    let mut updated = (**current_info).clone();
    let changes = BTreeMap::from([(key, value)]);
    let spectators_changed = match apply_gamerules(&mut updated.game_rules, &changes) {
        Ok(changed) => changed,
        Err(error) => return bad_request(error),
    };
    drop(current_info);
    server.level_info.store(Arc::new(updated));
    apply_gamerule_side_effects(server, spectators_changed);

    ApiResponse::ok(EmptyPayload {}).into_response()
}

fn gamerules_payload(registry: &GameRuleRegistry) -> BTreeMap<String, Value> {
    GameRule::all()
        .iter()
        .map(|rule| {
            let value = match registry.get(rule) {
                GameRuleValue::Int(value) => Value::from(*value),
                GameRuleValue::Bool(value) => Value::from(*value),
            };
            (rule.to_string(), value)
        })
        .collect()
}

fn apply_gamerules(
    registry: &mut GameRuleRegistry,
    changes: &BTreeMap<String, Value>,
) -> Result<bool, String> {
    let mut spectators_changed = false;
    for (key, value) in changes {
        let rule =
            find_gamerule(key).ok_or_else(|| format!("Cannot find the gamerule '{key}'."))?;
        match registry.get_mut(&rule) {
            GameRuleValue::Bool(target) => {
                let Some(value) = value.as_bool() else {
                    return Err(format!("Gamerule '{key}' requires a boolean value."));
                };
                if *target != value {
                    *target = value;
                    spectators_changed |= rule == GameRule::SpectatorsGenerateChunks;
                }
            }
            GameRuleValue::Int(target) => {
                let value = json_integer(value)
                    .ok_or_else(|| format!("Gamerule '{key}' requires a numeric value."))?;
                *target = value;
            }
        }
    }
    Ok(spectators_changed)
}

fn parse_query_value(
    registry: &GameRuleRegistry,
    rule: &GameRule,
    value: &str,
) -> Result<Value, String> {
    match registry.get(rule) {
        GameRuleValue::Bool(_) => match value {
            "true" => Ok(Value::Bool(true)),
            "false" => Ok(Value::Bool(false)),
            _ => Err("Boolean gamerule values must be 'true' or 'false'.".to_string()),
        },
        GameRuleValue::Int(_) => value
            .parse::<i64>()
            .map(Value::from)
            .map_err(|_| "Integer gamerule value is invalid.".to_string()),
    }
}

fn json_integer(value: &Value) -> Option<i64> {
    value
        .as_i64()
        .or_else(|| value.as_f64().map(|value| value as i64))
}

fn find_gamerule(key: &str) -> Option<GameRule> {
    GameRule::all()
        .iter()
        .find(|rule| rule.to_string() == key)
        .cloned()
}

fn apply_gamerule_side_effects(server: &Arc<Server>, spectators_changed: bool) {
    if !spectators_changed {
        return;
    }
    for world in server.worlds.load().iter() {
        for player in world.players.load().iter() {
            if player.is_spectator() {
                player.update_chunk_tickets_for_gamemode();
            }
        }
    }
}

fn is_dimension_name(name: &str) -> bool {
    matches!(name, "overworld" | "nether" | "the_end")
}

fn bad_request(message: impl Into<String>) -> Response {
    ApiError::new(StatusCode::BAD_REQUEST, message).into_response()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn payload_preserves_boolean_and_numeric_types() {
        let registry = GameRuleRegistry::default();
        let payload = gamerules_payload(&registry);

        assert_eq!(payload["keep_inventory"], Value::Bool(false));
        assert!(payload["random_tick_speed"].is_number());
    }

    #[test]
    fn batch_updates_are_typed_and_reject_unknown_rules() {
        let mut registry = GameRuleRegistry::default();
        let changes = BTreeMap::from([
            ("keep_inventory".to_string(), Value::Bool(true)),
            ("random_tick_speed".to_string(), Value::from(6)),
        ]);

        assert!(!apply_gamerules(&mut registry, &changes).unwrap());
        assert!(registry.keep_inventory);
        assert_eq!(registry.random_tick_speed, 6);
        assert!(
            apply_gamerules(
                &mut registry,
                &BTreeMap::from([("missing".to_string(), Value::Bool(true))])
            )
            .is_err()
        );
        assert!(
            apply_gamerules(
                &mut registry,
                &BTreeMap::from([("keep_inventory".to_string(), Value::from(1))])
            )
            .is_err()
        );
    }

    #[test]
    fn query_values_follow_the_existing_rule_type() {
        let registry = GameRuleRegistry::default();
        let keep_inventory = find_gamerule("keep_inventory").unwrap();
        let random_tick_speed = find_gamerule("random_tick_speed").unwrap();

        assert_eq!(
            parse_query_value(&registry, &keep_inventory, "true").unwrap(),
            Value::Bool(true)
        );
        assert!(parse_query_value(&registry, &keep_inventory, "1").is_err());
        assert_eq!(
            parse_query_value(&registry, &random_tick_speed, "12").unwrap(),
            Value::from(12)
        );
    }

    #[test]
    fn dimension_names_match_the_frontend_contract() {
        for dimension in ["overworld", "nether", "the_end"] {
            assert!(is_dimension_name(dimension));
        }
        for dimension in ["", "end", "minecraft:overworld"] {
            assert!(!is_dimension_name(dimension));
        }
    }
}
