use std::{
    fs, io,
    path::Path,
    sync::{Arc, PoisonError, atomic::Ordering},
};

use axum::{
    body::Bytes,
    extract::{Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use pumpkin::{data::whitelist::WhitelistConfig, server::Server};
use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::{
    opanel::OPanel,
    web::{
        controller::control::EmptyPayload,
        response::{ApiError, ApiResponse},
    },
};

const WHITELIST_PATH: &str = "data/whitelist.json";

#[derive(Clone, Debug, Deserialize, PartialEq, Eq, Serialize)]
struct WhitelistEntry {
    name: String,
    uuid: String,
}

#[derive(Debug, Serialize)]
struct WhitelistPayload {
    whitelist: Vec<WhitelistEntry>,
}

#[derive(Debug, Deserialize)]
pub(super) struct WhitelistEntryQuery {
    name: Option<String>,
    uuid: Option<String>,
}

#[derive(Debug, Error)]
enum PersistWhitelistError {
    #[error("invalid whitelist payload: {0}")]
    Invalid(#[from] serde_json::Error),
    #[error("failed to create the whitelist data directory: {0}")]
    CreateDirectory(#[source] io::Error),
    #[error("failed to write {WHITELIST_PATH}: {0}")]
    Write(#[source] io::Error),
}

pub(super) async fn get_whitelist(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = &opanel.context().server;
    let whitelist = server
        .data
        .whitelist_config
        .read()
        .unwrap_or_else(PoisonError::into_inner);

    ApiResponse::ok(WhitelistPayload {
        whitelist: whitelist_entries(&whitelist),
    })
    .into_response()
}

pub(super) async fn enable_whitelist(State(opanel): State<Arc<OPanel>>) -> Response {
    opanel
        .context()
        .server
        .white_list
        .store(true, Ordering::Relaxed);
    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn disable_whitelist(State(opanel): State<Arc<OPanel>>) -> Response {
    opanel
        .context()
        .server
        .white_list
        .store(false, Ordering::Relaxed);
    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn write_whitelist(State(opanel): State<Arc<OPanel>>, body: Bytes) -> Response {
    let entries: Vec<WhitelistEntry> = match serde_json::from_slice(&body) {
        Ok(entries) => entries,
        Err(error) => return bad_request(error.to_string()),
    };

    update_whitelist(&opanel.context().server, entries)
}

pub(super) async fn add_whitelist_entry(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<WhitelistEntryQuery>,
) -> Response {
    let Some(name) = query.name else {
        return bad_request("Missing name or uuid.");
    };
    let Some(uuid) = query.uuid else {
        return bad_request("Missing name or uuid.");
    };

    let server = &opanel.context().server;
    let mut whitelist = server
        .data
        .whitelist_config
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    let mut entries = whitelist_entries(&whitelist);
    if add_entry(&mut entries, WhitelistEntry { name, uuid })
        && let Err(error) = persist_whitelist(&mut whitelist, &entries)
    {
        return persistence_error(error);
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn remove_whitelist_entry(
    State(opanel): State<Arc<OPanel>>,
    Query(query): Query<WhitelistEntryQuery>,
) -> Response {
    let Some(name) = query.name else {
        return bad_request("Missing name or uuid.");
    };
    let Some(uuid) = query.uuid else {
        return bad_request("Missing name or uuid.");
    };

    let server = &opanel.context().server;
    let mut whitelist = server
        .data
        .whitelist_config
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    let mut entries = whitelist_entries(&whitelist);
    if remove_entry(&mut entries, &name, &uuid)
        && let Err(error) = persist_whitelist(&mut whitelist, &entries)
    {
        return persistence_error(error);
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

fn update_whitelist(server: &Server, entries: Vec<WhitelistEntry>) -> Response {
    let mut whitelist = server
        .data
        .whitelist_config
        .write()
        .unwrap_or_else(PoisonError::into_inner);
    match persist_whitelist(&mut whitelist, &entries) {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => persistence_error(error),
    }
}

fn whitelist_entries(whitelist: &WhitelistConfig) -> Vec<WhitelistEntry> {
    whitelist
        .whitelist
        .iter()
        .map(|entry| WhitelistEntry {
            name: entry.name.clone(),
            uuid: entry.uuid.to_string(),
        })
        .collect()
}

fn persist_whitelist(
    current: &mut WhitelistConfig,
    entries: &[WhitelistEntry],
) -> Result<(), PersistWhitelistError> {
    let (replacement, content) = serialize_whitelist(entries)?;

    let path = Path::new(WHITELIST_PATH);
    let parent = path.parent().expect("whitelist path should have a parent");
    fs::create_dir_all(parent).map_err(PersistWhitelistError::CreateDirectory)?;
    fs::write(path, content).map_err(PersistWhitelistError::Write)?;
    *current = replacement;
    Ok(())
}

fn serialize_whitelist(
    entries: &[WhitelistEntry],
) -> Result<(WhitelistConfig, String), serde_json::Error> {
    let value = serde_json::to_value(entries)?;
    let replacement: WhitelistConfig = serde_json::from_value(value)?;
    let content = serde_json::to_string_pretty(&replacement)?;
    Ok((replacement, content))
}

fn add_entry(entries: &mut Vec<WhitelistEntry>, entry: WhitelistEntry) -> bool {
    if entries.iter().any(|existing| existing.name == entry.name) {
        return false;
    }
    entries.push(entry);
    true
}

fn remove_entry(entries: &mut Vec<WhitelistEntry>, name: &str, uuid: &str) -> bool {
    if !entries.iter().any(|entry| entry.name == name) {
        return false;
    }

    let previous_len = entries.len();
    entries.retain(|entry| entry.uuid != uuid);
    entries.len() != previous_len
}

fn persistence_error(error: PersistWhitelistError) -> Response {
    let status = match error {
        PersistWhitelistError::Invalid(_) => StatusCode::BAD_REQUEST,
        PersistWhitelistError::CreateDirectory(_) | PersistWhitelistError::Write(_) => {
            StatusCode::INTERNAL_SERVER_ERROR
        }
    };
    ApiError::new(status, error.to_string()).into_response()
}

fn bad_request(message: impl Into<String>) -> Response {
    ApiError::new(StatusCode::BAD_REQUEST, message).into_response()
}

#[cfg(test)]
mod tests {
    use super::{WhitelistEntry, add_entry, remove_entry, serialize_whitelist};

    fn entry(name: &str, uuid: &str) -> WhitelistEntry {
        WhitelistEntry {
            name: name.to_owned(),
            uuid: uuid.to_owned(),
        }
    }

    #[test]
    fn add_ignores_an_existing_player_name() {
        let mut entries = vec![entry("Steve", "00000000-0000-0000-0000-000000000001")];

        assert!(!add_entry(
            &mut entries,
            entry("Steve", "00000000-0000-0000-0000-000000000002")
        ));
        assert_eq!(entries.len(), 1);
    }

    #[test]
    fn remove_requires_an_existing_name_and_removes_by_uuid() {
        let mut entries = vec![
            entry("Steve", "00000000-0000-0000-0000-000000000001"),
            entry("Alex", "00000000-0000-0000-0000-000000000002"),
        ];

        assert!(!remove_entry(
            &mut entries,
            "Missing",
            "00000000-0000-0000-0000-000000000001"
        ));
        assert!(remove_entry(
            &mut entries,
            "Steve",
            "00000000-0000-0000-0000-000000000001"
        ));
        assert_eq!(
            entries,
            vec![entry("Alex", "00000000-0000-0000-0000-000000000002")]
        );
    }

    #[test]
    fn serialization_rejects_invalid_uuids_before_writing() {
        let entries = vec![entry("Steve", "not-a-uuid")];

        assert!(serialize_whitelist(&entries).is_err());
    }
}
