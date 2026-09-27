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
use pumpkin::data::datapack::DatapackManager;
use pumpkin_nbt::{
    compound::NbtCompound,
    nbt_compress::{read_gzip_compound_tag, write_gzip_compound_tag},
    tag::NbtTag,
};
use pumpkin_util::{Difficulty, GameMode};
use serde::{Deserialize, Serialize};
use tokio::io::AsyncWriteExt;
use toml_edit::{DocumentMut, Item, Value, value};
use tracing::{error, warn};
use zip::{ZipArchive, ZipWriter, write::SimpleFileOptions};

use crate::{
    opanel::OPanel,
    utils::file::is_safe_file_name,
    web::{
        controller::control::EmptyPayload,
        response::{ApiError, ApiResponse},
    },
};

const PUMPKIN_CONFIG_PATH: &str = "config/pumpkin.toml";
const LEVEL_DATA_FILE: &str = "level.dat";

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

#[derive(Clone, Copy, Debug)]
struct SaveSettings {
    game_mode: GameMode,
    difficulty: Difficulty,
    difficulty_locked: bool,
    hardcore: bool,
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
    let server = &opanel.context().server;
    let running_save = server.basic_config.default_level_name.clone();
    let runtime_settings = SaveSettings {
        game_mode: server.basic_config.default_gamemode,
        difficulty: server.basic_config.default_difficulty,
        difficulty_locked: server.level_info.load().difficulty_locked,
        hardcore: server.basic_config.hardcore,
    };
    let (configured_save, configured_settings) =
        configured_save_configuration(runtime_settings).await;
    let current_save = configured_save.unwrap_or_else(|| running_save.clone());

    let scan = |running_save: String, current_save: String| {
        tokio::task::spawn_blocking(move || {
            scan_saves(&running_save, &current_save, configured_settings)
        })
    };
    let mut saves = match scan(running_save.clone(), current_save.clone()).await {
        Ok(Ok(saves)) => saves,
        Ok(Err(error)) => return internal_error("failed to scan saves", error),
        Err(error) => return internal_error("save scan task failed", error),
    };

    if saves.is_empty() {
        if let Err(error) = server.save_all().await {
            warn!(%error, "failed to save Pumpkin worlds before retrying save discovery");
        }
        saves = match scan(running_save, current_save).await {
            Ok(Ok(saves)) => saves,
            Ok(Err(error)) => return internal_error("failed to scan saves", error),
            Err(error) => return internal_error("save scan task failed", error),
        };
    }

    ApiResponse::ok(SavesPayload { saves }).into_response()
}

pub(super) async fn download_save(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
) -> Response {
    let save_path = match validated_save_path(&save_name) {
        Ok(path) => path,
        Err(error) => return error.into_response(),
    };
    let server = &opanel.context().server;
    if server.basic_config.default_level_name == save_name
        && let Err(error) = server.save_all().await
    {
        return internal_error("failed to save the running world", error);
    }

    let temporary_directory = std::env::temp_dir().join("opanel");
    if let Err(error) = tokio::fs::create_dir_all(&temporary_directory).await {
        return internal_error("failed to create the temporary directory", error);
    }
    let archive_path = match random_temporary_path(&temporary_directory, "zip") {
        Ok(path) => path,
        Err(error) => return internal_error("failed to create an archive name", error),
    };
    let archive_source = save_path.clone();
    let archive_target = archive_path.clone();
    match tokio::task::spawn_blocking(move || create_save_archive(&archive_source, &archive_target))
        .await
    {
        Ok(Ok(())) => {}
        Ok(Err(error)) => {
            let _ = tokio::fs::remove_file(&archive_path).await;
            return internal_error("failed to archive save", error);
        }
        Err(error) => {
            let _ = tokio::fs::remove_file(&archive_path).await;
            return internal_error("save archive task failed", error);
        }
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
    let save_path = match validated_save_path(&save_name) {
        Ok(path) => path,
        Err(error) => return error.into_response(),
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
    let requested_game_mode = match GameMode::from_str(&request.default_game_mode) {
        Ok(game_mode) => game_mode,
        Err(_) => return bad_request("Invalid default game mode."),
    };
    let requested_difficulty = match Difficulty::from_str(&request.difficulty) {
        Ok(difficulty) => difficulty,
        Err(_) => return bad_request("Invalid difficulty."),
    };
    let settings = if request.is_hardcore {
        SaveSettings {
            game_mode: GameMode::Survival,
            difficulty: Difficulty::Hard,
            difficulty_locked: true,
            hardcore: true,
        }
    } else {
        SaveSettings {
            game_mode: requested_game_mode,
            difficulty: requested_difficulty,
            difficulty_locked: request.is_difficulty_locked,
            hardcore: false,
        }
    };
    let metadata_path = save_path.clone();
    match tokio::task::spawn_blocking(move || {
        edit_save_metadata(&metadata_path, &display_name, settings)
    })
    .await
    {
        Ok(Ok(())) => {}
        Ok(Err(error)) => return internal_error("failed to edit save metadata", error),
        Err(error) => return internal_error("save metadata task failed", error),
    }

    let server = &opanel.context().server;
    let (configured_save, _) = configured_save_configuration(settings).await;
    let configured_save =
        configured_save.unwrap_or_else(|| server.basic_config.default_level_name.clone());
    if configured_save == save_name
        && let Err(error) = update_pumpkin_save_settings(settings).await
    {
        return internal_error("failed to update Pumpkin save settings", error);
    }
    if server.basic_config.default_level_name == save_name {
        let mut level_info = (**server.level_info.load()).clone();
        level_info.level_name = read_save_display_name(&save_path).unwrap_or(save_name);
        level_info.difficulty = settings.difficulty;
        level_info.difficulty_locked = settings.difficulty_locked;
        server.level_info.store(Arc::new(level_info));
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn toggle_save_datapack(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
    Query(query): Query<DatapackQuery>,
) -> Response {
    let save_path = match validated_save_path(&save_name) {
        Ok(path) => path,
        Err(error) => return error.into_response(),
    };
    let (Some(datapack), Some(enabled)) = (query.datapack, query.enabled) else {
        return bad_request("Datapack id or status is missing.");
    };
    if datapack == "vanilla" {
        return ApiError::new(StatusCode::FORBIDDEN, "Cannot toggle vanilla datapack.")
            .into_response();
    }
    let enable = enabled == "1";
    let metadata_path = save_path;
    let metadata_datapack = datapack.clone();
    let changed = match tokio::task::spawn_blocking(move || {
        toggle_datapack(&metadata_path, &metadata_datapack, enable)
    })
    .await
    {
        Ok(Ok(changed)) => changed,
        Ok(Err(error)) => return internal_error("failed to toggle save datapack", error),
        Err(error) => return internal_error("datapack task failed", error),
    };

    let server = &opanel.context().server;
    if changed && server.basic_config.default_level_name == save_name {
        let mut level_info = (**server.level_info.load()).clone();
        let source = if enable {
            &mut level_info.data_packs.disabled
        } else {
            &mut level_info.data_packs.enabled
        };
        source.retain(|entry| entry != &datapack);
        let target = if enable {
            &mut level_info.data_packs.enabled
        } else {
            &mut level_info.data_packs.disabled
        };
        if !target.contains(&datapack) {
            target.push(datapack);
        }
        server.level_info.store(Arc::new(level_info));
        if let Err(error) = DatapackManager::reload(server) {
            return internal_error("failed to reload Pumpkin datapacks", error);
        }
    }

    ApiResponse::ok(EmptyPayload {}).into_response()
}

pub(super) async fn delete_save(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(save_name): AxumPath<String>,
) -> Response {
    let save_path = match validated_save_path(&save_name) {
        Ok(path) => path,
        Err(error) => return error.into_response(),
    };
    let server = &opanel.context().server;
    let (configured_save, _) = configured_save_configuration(SaveSettings {
        game_mode: server.basic_config.default_gamemode,
        difficulty: server.basic_config.default_difficulty,
        difficulty_locked: server.level_info.load().difficulty_locked,
        hardcore: server.basic_config.hardcore,
    })
    .await;
    let configured_save =
        configured_save.unwrap_or_else(|| server.basic_config.default_level_name.clone());
    if server.basic_config.default_level_name == save_name || configured_save == save_name {
        return ApiError::new(StatusCode::FORBIDDEN, "You cannot delete current save.")
            .into_response();
    }

    match tokio::task::spawn_blocking(move || fs::remove_dir_all(save_path)).await {
        Ok(Ok(())) => ApiResponse::ok(EmptyPayload {}).into_response(),
        Ok(Err(error)) => internal_error("failed to delete save", error),
        Err(error) => internal_error("save deletion task failed", error),
    }
}

fn scan_saves(
    running_save: &str,
    current_save: &str,
    fallback: SaveSettings,
) -> Result<Vec<SavePayload>, String> {
    let mut saves = Vec::new();
    for entry in fs::read_dir(".").map_err(|error| error.to_string())? {
        let entry = entry.map_err(|error| error.to_string())?;
        let path = entry.path();
        let file_type = entry.file_type().map_err(|error| error.to_string())?;
        if !file_type.is_dir() || file_type.is_symlink() || !path.join(LEVEL_DATA_FILE).is_file() {
            continue;
        }
        let Some(name) = entry.file_name().to_str().map(ToOwned::to_owned) else {
            continue;
        };
        let mut metadata = read_save_metadata(&path, fallback)?;
        let is_running = name == running_save;
        let is_current = name == current_save;
        if is_current {
            metadata.game_mode = fallback.game_mode;
            metadata.hardcore = fallback.hardcore;
        }
        saves.push(SavePayload {
            display_name: BASE64_STANDARD.encode(metadata.display_name),
            path: name.clone(),
            size: directory_size(&path).map_err(|error| error.to_string())?,
            is_running,
            is_current,
            default_game_mode: metadata.game_mode.name().to_string(),
            difficulty: metadata.difficulty.name().to_string(),
            is_difficulty_locked: metadata.difficulty_locked,
            is_hardcore: metadata.hardcore,
            datapacks: metadata.datapacks,
            name,
        });
    }
    saves.sort_by(|left, right| left.name.cmp(&right.name));
    Ok(saves)
}

#[derive(Debug)]
struct SaveMetadata {
    display_name: String,
    game_mode: GameMode,
    difficulty: Difficulty,
    difficulty_locked: bool,
    hardcore: bool,
    datapacks: BTreeMap<String, bool>,
}

impl SaveMetadata {
    const fn settings(&self) -> SaveSettings {
        SaveSettings {
            game_mode: self.game_mode,
            difficulty: self.difficulty,
            difficulty_locked: self.difficulty_locked,
            hardcore: self.hardcore,
        }
    }
}

fn read_save_metadata(path: &Path, fallback: SaveSettings) -> Result<SaveMetadata, String> {
    let root = read_level_data(path)?;
    let data = root
        .get_compound("Data")
        .ok_or_else(|| "level.dat is missing the Data compound".to_string())?;
    let display_name = data
        .get_string("LevelName")
        .map(ToOwned::to_owned)
        .or_else(|| path.file_name()?.to_str().map(ToOwned::to_owned))
        .unwrap_or_default();
    let game_mode = data
        .get_int("GameType")
        .and_then(|value| GameMode::try_from(value).ok())
        .unwrap_or(fallback.game_mode);
    let modern_difficulty = data.get_compound("difficulty_settings");
    let difficulty = modern_difficulty
        .and_then(|settings| settings.get_string("difficulty"))
        .and_then(|value| Difficulty::from_str(value).ok())
        .or_else(|| {
            data.get_byte("Difficulty")
                .and_then(|value| difficulty_from_id(value).ok())
        })
        .unwrap_or(fallback.difficulty);
    let difficulty_locked = modern_difficulty
        .and_then(|settings| settings.get_bool("locked"))
        .or_else(|| data.get_bool("DifficultyLocked"))
        .unwrap_or(fallback.difficulty_locked);
    let hardcore = modern_difficulty
        .and_then(|settings| settings.get_bool("hardcore"))
        .or_else(|| data.get_bool("hardcore"))
        .unwrap_or(fallback.hardcore);
    let mut datapacks = BTreeMap::new();
    if let Some(packs) = data.get_compound("DataPacks") {
        for datapack in strings_from_nbt(packs.get_list("Disabled")) {
            datapacks.insert(datapack, false);
        }
        for datapack in strings_from_nbt(packs.get_list("Enabled")) {
            datapacks.insert(datapack, true);
        }
    }
    Ok(SaveMetadata {
        display_name,
        game_mode,
        difficulty,
        difficulty_locked,
        hardcore,
        datapacks,
    })
}

fn edit_save_metadata(
    path: &Path,
    display_name: &str,
    settings: SaveSettings,
) -> Result<(), String> {
    let mut root = read_level_data(path)?;
    let data = data_compound_mut(&mut root)?;
    data.put_string("LevelName", display_name.to_string());
    data.put_int("GameType", settings.game_mode as i32);
    data.put_byte("Difficulty", settings.difficulty as i8);
    data.put_bool("DifficultyLocked", settings.difficulty_locked);
    data.put_bool("hardcore", settings.hardcore);
    let mut modern = data
        .get_compound("difficulty_settings")
        .cloned()
        .unwrap_or_default();
    modern.put_string("difficulty", settings.difficulty.name().to_string());
    modern.put_bool("locked", settings.difficulty_locked);
    modern.put_bool("hardcore", settings.hardcore);
    data.put_compound("difficulty_settings", modern);
    write_level_data(path, root)
}

fn toggle_datapack(path: &Path, datapack: &str, enable: bool) -> Result<bool, String> {
    let mut root = read_level_data(path)?;
    let data = data_compound_mut(&mut root)?;
    let mut packs = data.get_compound("DataPacks").cloned().unwrap_or_default();
    let mut enabled = strings_from_nbt(packs.get_list("Enabled"));
    let mut disabled = strings_from_nbt(packs.get_list("Disabled"));
    let exists = enabled.iter().any(|entry| entry == datapack)
        || disabled.iter().any(|entry| entry == datapack);
    if !exists {
        return Ok(false);
    }
    let already_enabled = enabled.iter().any(|entry| entry == datapack);
    if already_enabled == enable {
        return Ok(false);
    }
    enabled.retain(|entry| entry != datapack);
    disabled.retain(|entry| entry != datapack);
    if enable {
        enabled.push(datapack.to_string());
    } else {
        disabled.push(datapack.to_string());
    }
    packs.put_list("Enabled", strings_to_nbt(&enabled));
    packs.put_list("Disabled", strings_to_nbt(&disabled));
    data.put_compound("DataPacks", packs);
    write_level_data(path, root)?;
    Ok(true)
}

fn read_level_data(path: &Path) -> Result<NbtCompound, String> {
    let file = File::open(path.join(LEVEL_DATA_FILE)).map_err(|error| error.to_string())?;
    read_gzip_compound_tag(file).map_err(|error| error.to_string())
}

fn write_level_data(path: &Path, root: NbtCompound) -> Result<(), String> {
    let level_data_path = path.join(LEVEL_DATA_FILE);
    let backup_path = path.join("level.dat_old");
    fs::copy(&level_data_path, backup_path).map_err(|error| error.to_string())?;
    let file = File::create(level_data_path).map_err(|error| error.to_string())?;
    write_gzip_compound_tag(root, file).map_err(|error| error.to_string())
}

fn data_compound_mut(root: &mut NbtCompound) -> Result<&mut NbtCompound, String> {
    match root.child_tags.get_mut("Data") {
        Some(NbtTag::Compound(data)) => Ok(data),
        _ => Err("level.dat is missing the Data compound".to_string()),
    }
}

fn read_save_display_name(path: &Path) -> Option<String> {
    read_level_data(path)
        .ok()?
        .get_compound("Data")?
        .get_string("LevelName")
        .map(ToOwned::to_owned)
}

fn strings_from_nbt(tags: Option<&[NbtTag]>) -> Vec<String> {
    tags.unwrap_or_default()
        .iter()
        .filter_map(|tag| tag.extract_string().map(ToOwned::to_owned))
        .collect()
}

fn strings_to_nbt(values: &[String]) -> Vec<NbtTag> {
    values
        .iter()
        .map(|value| NbtTag::from(value.as_str()))
        .collect()
}

fn difficulty_from_id(value: i8) -> Result<Difficulty, ()> {
    match value {
        0 => Ok(Difficulty::Peaceful),
        1 => Ok(Difficulty::Easy),
        2 => Ok(Difficulty::Normal),
        3 => Ok(Difficulty::Hard),
        _ => Err(()),
    }
}

fn validated_save_path(save_name: &str) -> Result<PathBuf, ApiError> {
    if !is_safe_file_name(save_name) {
        return Err(ApiError::new(StatusCode::BAD_REQUEST, "Illegal save name."));
    }
    let path = PathBuf::from(save_name);
    let valid = fs::symlink_metadata(&path)
        .map(|metadata| metadata.is_dir() && !metadata.file_type().is_symlink())
        .unwrap_or(false)
        && path.join(LEVEL_DATA_FILE).is_file();
    if !valid {
        return Err(ApiError::new(
            StatusCode::NOT_FOUND,
            "Cannot find the specified save.",
        ));
    }
    Ok(path)
}

fn directory_size(path: &Path) -> std::io::Result<u64> {
    let mut size = 0;
    for entry in fs::read_dir(path)? {
        let entry = entry?;
        let metadata = fs::symlink_metadata(entry.path())?;
        if metadata.file_type().is_symlink() {
            continue;
        }
        if metadata.is_dir() {
            size += directory_size(&entry.path())?;
        } else if metadata.is_file() {
            size += metadata.len();
        }
    }
    Ok(size)
}

fn create_save_archive(source: &Path, target: &Path) -> Result<(), ArchiveError> {
    let root_name = source
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or(ArchiveError::InvalidSave)?;
    let file = File::create(target)?;
    let mut writer = ZipWriter::new(file);
    add_directory_to_archive(&mut writer, source, root_name)?;
    writer.finish()?;
    Ok(())
}

fn add_directory_to_archive(
    writer: &mut ZipWriter<File>,
    directory: &Path,
    archive_prefix: &str,
) -> Result<(), ArchiveError> {
    let options = SimpleFileOptions::default().compression_method(zip::CompressionMethod::Deflated);
    for entry in fs::read_dir(directory)? {
        let entry = entry?;
        let name = entry.file_name();
        let Some(name) = name.to_str() else {
            continue;
        };
        if name.ends_with("session.lock") {
            continue;
        }
        let metadata = fs::symlink_metadata(entry.path())?;
        if metadata.file_type().is_symlink() {
            continue;
        }
        let archive_name = format!("{archive_prefix}/{name}");
        if metadata.is_dir() {
            writer.add_directory(format!("{archive_name}/"), options)?;
            add_directory_to_archive(writer, &entry.path(), &archive_name)?;
        } else if metadata.is_file() {
            writer.start_file(archive_name, options)?;
            let mut input = File::open(entry.path())?;
            std::io::copy(&mut input, writer)?;
        }
    }
    Ok(())
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

fn random_temporary_path(directory: &Path, extension: &str) -> std::io::Result<PathBuf> {
    let mut random = [0_u8; 16];
    getrandom::fill(&mut random).map_err(std::io::Error::other)?;
    let name: String = random.iter().map(|byte| format!("{byte:02x}")).collect();
    Ok(directory.join(format!("{name}.{extension}")))
}

pub(super) async fn select_save(
    save_name: &str,
    fallback_game_mode: GameMode,
    fallback_difficulty: Difficulty,
    fallback_difficulty_locked: bool,
    fallback_hardcore: bool,
) -> Result<(), std::io::Error> {
    let save_path = PathBuf::from(save_name);
    let fallback = SaveSettings {
        game_mode: fallback_game_mode,
        difficulty: fallback_difficulty,
        difficulty_locked: fallback_difficulty_locked,
        hardcore: fallback_hardcore,
    };
    let metadata = tokio::task::spawn_blocking(move || read_save_metadata(&save_path, fallback))
        .await
        .map_err(std::io::Error::other)?
        .map_err(|error| std::io::Error::new(std::io::ErrorKind::InvalidData, error))?;
    let contents = tokio::fs::read_to_string(PUMPKIN_CONFIG_PATH).await?;
    let mut document = contents
        .parse::<DocumentMut>()
        .map_err(|error| std::io::Error::new(std::io::ErrorKind::InvalidData, error))?;
    set_save_configuration(&mut document, Some(save_name), metadata.settings());
    tokio::fs::write(PUMPKIN_CONFIG_PATH, document.to_string()).await
}

async fn configured_save_configuration(fallback: SaveSettings) -> (Option<String>, SaveSettings) {
    let Ok(contents) = tokio::fs::read_to_string(PUMPKIN_CONFIG_PATH).await else {
        return (None, fallback);
    };
    let Ok(document) = contents.parse::<DocumentMut>() else {
        return (None, fallback);
    };
    let save_name = document
        .get("default_level_name")
        .and_then(Item::as_str)
        .map(ToOwned::to_owned);
    let game_mode = document
        .get("default_gamemode")
        .and_then(Item::as_str)
        .and_then(parse_config_game_mode)
        .unwrap_or(fallback.game_mode);
    let difficulty = document
        .get("default_difficulty")
        .and_then(Item::as_str)
        .and_then(parse_config_difficulty)
        .unwrap_or(fallback.difficulty);
    let hardcore = document
        .get("hardcore")
        .and_then(Item::as_bool)
        .unwrap_or(fallback.hardcore);
    (
        save_name,
        SaveSettings {
            game_mode,
            difficulty,
            difficulty_locked: fallback.difficulty_locked,
            hardcore,
        },
    )
}

async fn update_pumpkin_save_settings(settings: SaveSettings) -> Result<(), std::io::Error> {
    let contents = tokio::fs::read_to_string(PUMPKIN_CONFIG_PATH).await?;
    let mut document = contents
        .parse::<DocumentMut>()
        .map_err(|error| std::io::Error::new(std::io::ErrorKind::InvalidData, error))?;
    set_save_configuration(&mut document, None, settings);
    tokio::fs::write(PUMPKIN_CONFIG_PATH, document.to_string()).await
}

fn set_save_configuration(
    document: &mut DocumentMut,
    save_name: Option<&str>,
    settings: SaveSettings,
) {
    if let Some(save_name) = save_name {
        set_toml_value(document, "default_level_name", Value::from(save_name));
    }
    set_toml_value(
        document,
        "default_gamemode",
        Value::from(settings.game_mode.to_str()),
    );
    let difficulty = match settings.difficulty {
        Difficulty::Peaceful => "Peaceful",
        Difficulty::Easy => "Easy",
        Difficulty::Normal => "Normal",
        Difficulty::Hard => "Hard",
    };
    set_toml_value(document, "default_difficulty", Value::from(difficulty));
    set_toml_value(document, "hardcore", Value::from(settings.hardcore));
}

fn parse_config_game_mode(value: &str) -> Option<GameMode> {
    GameMode::from_str(&value.to_ascii_lowercase()).ok()
}

fn parse_config_difficulty(value: &str) -> Option<Difficulty> {
    Difficulty::from_str(&value.to_ascii_lowercase()).ok()
}

fn set_toml_value(document: &mut DocumentMut, key: &str, new_value: Value) {
    let item = document
        .as_table_mut()
        .entry(key)
        .or_insert_with(|| value(new_value.clone()));
    let decor = item.as_value().map(|old_value| old_value.decor().clone());
    *item = Item::Value(new_value);
    if let (Some(decor), Some(value)) = (decor, item.as_value_mut()) {
        *value.decor_mut() = decor;
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

    use super::*;

    fn temporary_directory(label: &str) -> PathBuf {
        let path = random_temporary_path(&std::env::temp_dir(), label).unwrap();
        fs::create_dir_all(&path).unwrap();
        path
    }

    fn write_test_level_data(path: &Path) {
        fs::create_dir_all(path).unwrap();
        let mut data = NbtCompound::new();
        data.put_string("LevelName", "Original".to_string());
        data.put_int("GameType", GameMode::Creative as i32);
        data.put_byte("Difficulty", Difficulty::Easy as i8);
        data.put_bool("DifficultyLocked", false);
        data.put_bool("hardcore", false);
        data.put_long("UnknownField", 42);
        let mut packs = NbtCompound::new();
        packs.put_list(
            "Enabled",
            vec![NbtTag::from("vanilla"), NbtTag::from("file/example")],
        );
        packs.put_list("Disabled", vec![NbtTag::from("file/disabled")]);
        data.put_compound("DataPacks", packs);
        let mut root = NbtCompound::new();
        root.put_compound("Data", data);
        write_gzip_compound_tag(root, File::create(path.join(LEVEL_DATA_FILE)).unwrap()).unwrap();
    }

    #[test]
    fn editing_metadata_preserves_unknown_fields_and_enforces_hardcore_settings() {
        let directory = temporary_directory("save-metadata");
        write_test_level_data(&directory);
        let settings = SaveSettings {
            game_mode: GameMode::Survival,
            difficulty: Difficulty::Hard,
            difficulty_locked: true,
            hardcore: true,
        };

        edit_save_metadata(&directory, "Renamed", settings).unwrap();

        let root = read_level_data(&directory).unwrap();
        let data = root.get_compound("Data").unwrap();
        assert_eq!(data.get_string("LevelName"), Some("Renamed"));
        assert_eq!(data.get_int("GameType"), Some(0));
        assert_eq!(data.get_byte("Difficulty"), Some(3));
        assert_eq!(data.get_bool("DifficultyLocked"), Some(true));
        assert_eq!(data.get_bool("hardcore"), Some(true));
        assert_eq!(data.get_long("UnknownField"), Some(42));
        let modern = data.get_compound("difficulty_settings").unwrap();
        assert_eq!(modern.get_string("difficulty"), Some("hard"));
        assert_eq!(modern.get_bool("locked"), Some(true));
        assert_eq!(modern.get_bool("hardcore"), Some(true));
        assert!(directory.join("level.dat_old").is_file());
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn archive_round_trip_accepts_a_nested_save_folder_and_skips_session_lock() {
        let source_parent = temporary_directory("archive-source");
        let source = source_parent.join("world");
        write_test_level_data(&source);
        fs::write(source.join("notes.txt"), "hello").unwrap();
        fs::write(source.join("session.lock"), "locked").unwrap();
        let archive = source_parent.join("world.zip");
        create_save_archive(&source, &archive).unwrap();

        let extraction_parent = temporary_directory("archive-target");
        let target = extraction_parent.join("world");
        extract_save_archive(&archive, &target, "world").unwrap();

        assert!(target.join(LEVEL_DATA_FILE).is_file());
        assert_eq!(
            fs::read_to_string(target.join("notes.txt")).unwrap(),
            "hello"
        );
        assert!(!target.join("session.lock").exists());
        fs::remove_dir_all(source_parent).unwrap();
        fs::remove_dir_all(extraction_parent).unwrap();
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

    #[test]
    fn toggling_a_known_datapack_moves_it_between_lists() {
        let directory = temporary_directory("datapack");
        write_test_level_data(&directory);

        assert!(toggle_datapack(&directory, "file/example", false).unwrap());
        assert!(!toggle_datapack(&directory, "file/missing", true).unwrap());

        let metadata = read_save_metadata(
            &directory,
            SaveSettings {
                game_mode: GameMode::Survival,
                difficulty: Difficulty::Normal,
                difficulty_locked: false,
                hardcore: false,
            },
        )
        .unwrap();
        assert_eq!(metadata.datapacks.get("file/example"), Some(&false));
        assert_eq!(metadata.datapacks.get("vanilla"), Some(&true));
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn selected_save_configuration_preserves_unrelated_settings_and_comments() {
        let source = r#"# server
default_level_name = "world" # keep this comment
default_gamemode = "Survival"
default_difficulty = "Normal"
hardcore = false

[networking.java]
motd = "Hello"
"#;
        let mut document = source.parse::<DocumentMut>().unwrap();
        set_save_configuration(
            &mut document,
            Some("new world"),
            SaveSettings {
                game_mode: GameMode::Adventure,
                difficulty: Difficulty::Hard,
                difficulty_locked: true,
                hardcore: true,
            },
        );
        let updated = document.to_string();
        let document = updated.parse::<DocumentMut>().unwrap();

        assert_eq!(document["default_level_name"].as_str(), Some("new world"));
        assert_eq!(document["default_gamemode"].as_str(), Some("Adventure"));
        assert_eq!(document["default_difficulty"].as_str(), Some("Hard"));
        assert_eq!(document["hardcore"].as_bool(), Some(true));
        assert_eq!(
            document["networking"]["java"]["motd"].as_str(),
            Some("Hello")
        );
        assert!(updated.contains("# keep this comment"));
    }
}
