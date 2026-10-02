use std::{
    collections::BTreeMap,
    fs::{self, File},
    path::{Path, PathBuf},
    str::FromStr,
    sync::Arc,
};

use axum::{
    body::Bytes,
    extract::{Multipart, Path as AxumPath, Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64_STANDARD};
use pumpkin_util::{Difficulty, GameMode};
use serde::{Deserialize, Serialize};
use tokio::io::AsyncWriteExt;
use tracing::{error, warn};
use zip::ZipArchive;

use crate::{
    opanel::OPanel,
    save::{LEVEL_DATA_FILE, Save, SaveEdit, SaveError, SaveSnapshot},
    utils::file::{is_safe_file_name, random_temporary_path},
    web::{
        controller::control::EmptyPayload,
        response::{ApiError, ApiResponse},
    },
};

#[derive(Debug, Serialize)]
struct SavesPayload {
    saves: Vec<SavePayload>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct SavePayload {
    name: String,
    display_name: String,
    path: String,
    size: u64,
    is_running: bool,
    is_current: bool,
    default_game_mode: String,
    difficulty: String,
    is_difficulty_locked: bool,
    is_hardcore: bool,
    datapacks: BTreeMap<String, bool>,
}

impl From<SaveSnapshot> for SavePayload {
    fn from(snapshot: SaveSnapshot) -> Self {
        Self {
            name: snapshot.name,
            display_name: BASE64_STANDARD.encode(snapshot.display_name),
            path: snapshot.path,
            size: snapshot.size,
            is_running: snapshot.is_running,
            is_current: snapshot.is_current,
            default_game_mode: snapshot.game_mode.name().to_string(),
            difficulty: snapshot.difficulty.name().to_string(),
            is_difficulty_locked: snapshot.difficulty_locked,
            is_hardcore: snapshot.hardcore,
            datapacks: snapshot.datapacks,
        }
    }
}

#[derive(Debug, Serialize)]
struct DownloadPayload {
    download: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct SaveEditRequest {
    display_name: String,
    default_game_mode: String,
    difficulty: String,
    is_difficulty_locked: bool,
    is_hardcore: bool,
}

#[derive(Debug, Deserialize)]
pub(super) struct DatapackQuery {
    datapack: Option<String>,
    enabled: Option<String>,
}

#[derive(Debug, thiserror::Error)]
enum ArchiveError {
    #[error(transparent)]
    Io(#[from] std::io::Error),
    #[error(transparent)]
    Zip(#[from] zip::result::ZipError),
    #[error("archive contains an unsafe path")]
    UnsafePath,
    #[error("save name conflict")]
    Conflict,
    #[error("archive does not contain a valid save")]
    InvalidSave,
}

pub(super) async fn get_saves(State(opanel): State<Arc<OPanel>>) -> Response {
    let server = Arc::clone(&opanel.context().server);
    let mut saves = match Save::list(Arc::clone(&server)).await {
        Ok(saves) => saves,
        Err(error) => return save_error("failed to scan saves", error),
    };

    if saves.is_empty() {
        if let Err(error) = server.save_all().await {
            warn!(%error, "failed to save Pumpkin worlds before retrying save discovery");
        }
        saves = match Save::list(server).await {
            Ok(saves) => saves,
            Err(error) => return save_error("failed to scan saves", error),
        };
    }

    ApiResponse::ok(SavesPayload {
        saves: saves.into_iter().map(SavePayload::from).collect(),
    })
    .into_response()
}

pub(super) async fn download_save(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
) -> Response {
    let save = match open_save(&opanel, &save_name).await {
        Ok(save) => save,
        Err(error) => return save_error("failed to open save", error),
    };
    let temporary_directory = std::env::temp_dir().join("opanel");
    if let Err(error) = tokio::fs::create_dir_all(&temporary_directory).await {
        return internal_error("failed to create the temporary directory", error);
    }
    let archive_path = match random_temporary_path(&temporary_directory, "zip") {
        Ok(path) => path,
        Err(error) => return internal_error("failed to create an archive name", error),
    };
    if let Err(error) = save.archive_to(&archive_path).await {
        let _ = tokio::fs::remove_file(&archive_path).await;
        return save_error("failed to archive save", error);
    }

    let download = match opanel
        .downloads()
        .register_path(archive_path.clone(), true)
        .await
    {
        Ok(download) => download,
        Err(error) => {
            let _ = tokio::fs::remove_file(&archive_path).await;
            return internal_error("failed to register save download", error);
        }
    };
    ApiResponse::ok(DownloadPayload { download }).into_response()
}

pub(super) async fn upload_save(
    State(_opanel): State<Arc<OPanel>>,
    mut multipart: Multipart,
) -> Response {
    let temporary_directory = std::env::temp_dir().join("opanel");
    if let Err(error) = tokio::fs::create_dir_all(&temporary_directory).await {
        return internal_error("failed to create the temporary directory", error);
    }

    while let Some(mut field) = match multipart.next_field().await {
        Ok(field) => field,
        Err(error) => return bad_request(error.to_string()),
    } {
        if field.name() != Some("file") {
            continue;
        }
        let Some(file_name) = field.file_name().map(ToOwned::to_owned) else {
            return bad_request("Illegal save file name.");
        };
        let Some(save_name) = file_name.strip_suffix(".zip") else {
            return bad_request("Save file should be a zip.");
        };
        if !is_safe_file_name(&file_name) || !is_safe_file_name(save_name) {
            return bad_request("Illegal save file name.");
        }
        let save_name = save_name.to_string();
        let target = PathBuf::from(&save_name);
        if target.exists() {
            return ApiError::new(StatusCode::CONFLICT, "Save name conflict.").into_response();
        }

        let temporary_archive = match random_temporary_path(&temporary_directory, "upload") {
            Ok(path) => path,
            Err(error) => return internal_error("failed to create an upload name", error),
        };
        let mut output = match tokio::fs::File::create(&temporary_archive).await {
            Ok(output) => output,
            Err(error) => return internal_error("failed to create the uploaded archive", error),
        };
        let mut uploaded_size = 0_u64;
        loop {
            match field.chunk().await {
                Ok(Some(chunk)) => {
                    uploaded_size += chunk.len() as u64;
                    if let Err(error) = output.write_all(&chunk).await {
                        let _ = tokio::fs::remove_file(&temporary_archive).await;
                        return internal_error("failed to write the uploaded archive", error);
                    }
                }
                Ok(None) => break,
                Err(error) => {
                    let _ = tokio::fs::remove_file(&temporary_archive).await;
                    return bad_request(error.to_string());
                }
            }
        }
        drop(output);
        if uploaded_size == 0 {
            let _ = tokio::fs::remove_file(&temporary_archive).await;
            return bad_request("File is missing.");
        }

        let archive = temporary_archive.clone();
        let extraction_target = target.clone();
        let extraction_name = save_name.clone();
        let result = tokio::task::spawn_blocking(move || {
            extract_save_archive(&archive, &extraction_target, &extraction_name)
        })
        .await;
        let _ = tokio::fs::remove_file(&temporary_archive).await;
        return match result {
            Ok(Ok(())) => ApiResponse::ok(EmptyPayload {}).into_response(),
            Ok(Err(ArchiveError::Conflict)) => {
                ApiError::new(StatusCode::CONFLICT, "Save name conflict.").into_response()
            }
            Ok(Err(ArchiveError::UnsafePath | ArchiveError::Zip(_))) => ApiError::new(
                StatusCode::FORBIDDEN,
                "Invalid save file, zip slip detected.",
            )
            .into_response(),
            Ok(Err(ArchiveError::InvalidSave)) => bad_request("Invalid save file."),
            Ok(Err(error)) => internal_error("failed to extract the uploaded save", error),
            Err(error) => internal_error("save extraction task failed", error),
        };
    }

    bad_request("File is missing.")
}

pub(super) async fn edit_save(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
    body: Bytes,
) -> Response {
    let save = match open_save(&opanel, &save_name).await {
        Ok(save) => save,
        Err(error) => return save_error("failed to open save", error),
    };
    let request: SaveEditRequest = match serde_json::from_slice(&body) {
        Ok(request) => request,
        Err(error) => return bad_request(error.to_string()),
    };
    let display_name_bytes = match BASE64_STANDARD.decode(request.display_name) {
        Ok(display_name) => display_name,
        Err(error) => return bad_request(format!("Invalid display name: {error}")),
    };
    let display_name = match String::from_utf8(display_name_bytes) {
        Ok(display_name) => display_name,
        Err(error) => return bad_request(format!("Invalid display name: {error}")),
    };
    let game_mode = match GameMode::from_str(&request.default_game_mode) {
        Ok(game_mode) => game_mode,
        Err(_) => return bad_request("Invalid default game mode."),
    };
    let difficulty = match Difficulty::from_str(&request.difficulty) {
        Ok(difficulty) => difficulty,
        Err(_) => return bad_request("Invalid difficulty."),
    };
    let edit = SaveEdit::new(
        display_name,
        game_mode,
        difficulty,
        request.is_difficulty_locked,
        request.is_hardcore,
    );

    match save.apply_edit(edit).await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => save_error("failed to edit save", error),
    }
}

pub(super) async fn toggle_save_datapack(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
    Query(query): Query<DatapackQuery>,
) -> Response {
    let save = match open_save(&opanel, &save_name).await {
        Ok(save) => save,
        Err(error) => return save_error("failed to open save", error),
    };
    let (Some(datapack), Some(enabled)) = (query.datapack, query.enabled) else {
        return bad_request("Datapack id or status is missing.");
    };
    if datapack == "vanilla" {
        return ApiError::new(StatusCode::FORBIDDEN, "Cannot toggle vanilla datapack.")
            .into_response();
    }
    let enable = enabled == "1";

    match save.toggle_datapack(datapack, enable).await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => save_error("failed to toggle save datapack", error),
    }
}

pub(super) async fn delete_save(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
) -> Response {
    let save = match open_save(&opanel, &save_name).await {
        Ok(save) => save,
        Err(error) => return save_error("failed to open save", error),
    };
    match save.delete().await {
        Ok(()) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Err(error) => save_error("failed to delete save", error),
    }
}

async fn open_save(opanel: &OPanel, save_name: &str) -> Result<Save, SaveError> {
    Save::open(Arc::clone(&opanel.context().server), save_name).await
}

fn extract_save_archive(
    archive_path: &Path,
    target: &Path,
    save_name: &str,
) -> Result<(), ArchiveError> {
    match fs::create_dir(target) {
        Ok(()) => {}
        Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => {
            return Err(ArchiveError::Conflict);
        }
        Err(error) => return Err(error.into()),
    }
    let result = extract_save_archive_inner(archive_path, target, save_name);
    if result.is_err() {
        let _ = fs::remove_dir_all(target);
    }
    result
}

fn extract_save_archive_inner(
    archive_path: &Path,
    target: &Path,
    save_name: &str,
) -> Result<(), ArchiveError> {
    let mut archive = ZipArchive::new(File::open(archive_path)?)?;
    for index in 0..archive.len() {
        let mut entry = archive.by_index(index)?;
        let enclosed = entry.enclosed_name().ok_or(ArchiveError::UnsafePath)?;
        let output_path = target.join(enclosed);
        if entry.is_dir() {
            fs::create_dir_all(&output_path)?;
            continue;
        }
        if let Some(parent) = output_path.parent() {
            fs::create_dir_all(parent)?;
        }
        let mut output = File::create(output_path)?;
        std::io::copy(&mut entry, &mut output)?;
    }

    if !target.join(LEVEL_DATA_FILE).is_file() {
        let nested = target.join(save_name);
        if !nested.is_dir() {
            return Err(ArchiveError::InvalidSave);
        }
        for entry in fs::read_dir(&nested)? {
            let entry = entry?;
            fs::rename(entry.path(), target.join(entry.file_name()))?;
        }
        fs::remove_dir(nested)?;
    }
    if !target.join(LEVEL_DATA_FILE).is_file() {
        return Err(ArchiveError::InvalidSave);
    }
    Ok(())
}

fn save_error(context: &str, error: SaveError) -> Response {
    match error {
        SaveError::InvalidName => bad_request("Illegal save name."),
        SaveError::NotFound => {
            ApiError::new(StatusCode::NOT_FOUND, "Cannot find the specified save.").into_response()
        }
        SaveError::ActiveSave => {
            ApiError::new(StatusCode::FORBIDDEN, "You cannot delete current save.").into_response()
        }
        error => internal_error(context, error),
    }
}

fn bad_request(message: impl Into<String>) -> Response {
    ApiError::new(StatusCode::BAD_REQUEST, message).into_response()
}

fn internal_error(context: &str, error: impl std::fmt::Display) -> Response {
    error!(%error, %context);
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
}

#[cfg(test)]
mod tests {
    use std::io::Write;

    use zip::{ZipWriter, write::SimpleFileOptions};

    use super::*;

    fn temporary_directory(label: &str) -> PathBuf {
        let path = random_temporary_path(&std::env::temp_dir(), label).unwrap();
        fs::create_dir_all(&path).unwrap();
        path
    }

    #[test]
    fn extraction_accepts_an_archive_with_a_nested_save_folder() {
        let directory = temporary_directory("archive-target");
        let archive_path = directory.join("world.zip");
        let mut writer = ZipWriter::new(File::create(&archive_path).unwrap());
        writer
            .start_file("world/level.dat", SimpleFileOptions::default())
            .unwrap();
        writer.write_all(b"level data").unwrap();
        writer
            .start_file("world/notes.txt", SimpleFileOptions::default())
            .unwrap();
        writer.write_all(b"hello").unwrap();
        writer.finish().unwrap();
        let target = directory.join("world");

        extract_save_archive(&archive_path, &target, "world").unwrap();

        assert!(target.join(LEVEL_DATA_FILE).is_file());
        assert_eq!(
            fs::read_to_string(target.join("notes.txt")).unwrap(),
            "hello"
        );
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn extraction_rejects_zip_slip_and_removes_partial_target() {
        let directory = temporary_directory("zip-slip");
        let archive_path = directory.join("unsafe.zip");
        let mut writer = ZipWriter::new(File::create(&archive_path).unwrap());
        writer
            .start_file("../outside.txt", SimpleFileOptions::default())
            .unwrap();
        writer.write_all(b"unsafe").unwrap();
        writer.finish().unwrap();
        let target = directory.join("world");

        let error = extract_save_archive(&archive_path, &target, "world").unwrap_err();

        assert!(matches!(error, ArchiveError::UnsafePath));
        assert!(!target.exists());
        assert!(!directory.join("outside.txt").exists());
        fs::remove_dir_all(directory).unwrap();
    }
}
