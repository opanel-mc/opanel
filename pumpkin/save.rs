use std::{
    collections::BTreeMap,
    fs::{self, File},
    io,
    path::{Path, PathBuf},
    str::FromStr,
    sync::Arc,
};

use pumpkin::{data::datapack::DatapackManager, server::Server};
use pumpkin_nbt::{
    compound::NbtCompound,
    nbt_compress::{read_gzip_compound_tag, write_gzip_compound_tag},
    tag::NbtTag,
};
use pumpkin_util::{Difficulty, GameMode};
use thiserror::Error;
use toml_edit::{DocumentMut, Item, Value, value};
use zip::{ZipWriter, write::SimpleFileOptions};

use crate::utils::{
    file::{absolute_path_string, is_safe_file_name},
    pumpkin_config,
};

pub(crate) const LEVEL_DATA_FILE: &str = "level.dat";

#[derive(Clone, Copy, Debug)]
pub(crate) struct SaveSettings {
    pub(crate) game_mode: GameMode,
    pub(crate) difficulty: Difficulty,
    pub(crate) difficulty_locked: bool,
    pub(crate) hardcore: bool,
}

impl SaveSettings {
    pub(crate) fn from_server(server: &Server) -> Self {
        Self {
            game_mode: server.basic_config.default_gamemode,
            difficulty: server.basic_config.default_difficulty,
            difficulty_locked: server.level_info.load().difficulty_locked,
            hardcore: server.basic_config.hardcore,
        }
    }

    fn normalized(
        game_mode: GameMode,
        difficulty: Difficulty,
        difficulty_locked: bool,
        hardcore: bool,
    ) -> Self {
        if hardcore {
            Self {
                game_mode: GameMode::Survival,
                difficulty: Difficulty::Hard,
                difficulty_locked: true,
                hardcore: true,
            }
        } else {
            Self {
                game_mode,
                difficulty,
                difficulty_locked,
                hardcore: false,
            }
        }
    }
}

#[derive(Clone, Debug)]
pub(crate) struct SaveEdit {
    display_name: String,
    settings: SaveSettings,
}

impl SaveEdit {
    pub(crate) fn new(
        display_name: String,
        game_mode: GameMode,
        difficulty: Difficulty,
        difficulty_locked: bool,
        hardcore: bool,
    ) -> Self {
        Self {
            display_name,
            settings: SaveSettings::normalized(game_mode, difficulty, difficulty_locked, hardcore),
        }
    }
}

#[derive(Debug)]
pub(crate) struct SaveSnapshot {
    pub(crate) name: String,
    pub(crate) display_name: String,
    pub(crate) path: String,
    pub(crate) size: u64,
    pub(crate) is_running: bool,
    pub(crate) is_current: bool,
    pub(crate) game_mode: GameMode,
    pub(crate) difficulty: Difficulty,
    pub(crate) difficulty_locked: bool,
    pub(crate) hardcore: bool,
    pub(crate) datapacks: BTreeMap<String, bool>,
}

#[derive(Debug, Error)]
pub(crate) enum SaveError {
    #[error("illegal save name")]
    InvalidName,
    #[error("cannot find the specified save")]
    NotFound,
    #[error("cannot delete the running or configured save")]
    ActiveSave,
    #[error("invalid level.dat: {0}")]
    InvalidLevelData(String),
    #[error("Pumpkin server operation failed: {0}")]
    Server(String),
    #[error("Pumpkin datapack reload failed: {0}")]
    Datapack(String),
    #[error(transparent)]
    Io(#[from] io::Error),
    #[error(transparent)]
    Zip(#[from] zip::result::ZipError),
    #[error(transparent)]
    Task(#[from] tokio::task::JoinError),
}

#[derive(Clone)]
pub(crate) struct Save {
    server: Arc<Server>,
    name: String,
    path: PathBuf,
    current_name: Arc<str>,
    fallback: SaveSettings,
}

impl Save {
    pub(crate) async fn open(server: Arc<Server>, name: &str) -> Result<Self, SaveError> {
        if !is_safe_file_name(name) {
            return Err(SaveError::InvalidName);
        }
        let name = name.to_string();
        let path_name = name.clone();
        let path = tokio::task::spawn_blocking(move || validate_save_path(&path_name)).await??;
        let configuration = SaveConfiguration::read(&server).await?;
        Ok(Self::new(server, name, path, configuration))
    }

    pub(crate) async fn list(server: Arc<Server>) -> Result<Vec<SaveSnapshot>, SaveError> {
        let configuration = SaveConfiguration::read(&server).await?;
        tokio::task::spawn_blocking(move || {
            let mut saves = Vec::new();
            for entry in fs::read_dir(".")? {
                let entry = entry?;
                let path = entry.path();
                let file_type = entry.file_type()?;
                if !file_type.is_dir()
                    || file_type.is_symlink()
                    || !path.join(LEVEL_DATA_FILE).is_file()
                {
                    continue;
                }
                let Some(name) = entry.file_name().to_str().map(ToOwned::to_owned) else {
                    continue;
                };
                let save = Self::new(server.clone(), name, path, configuration.clone());
                saves.push(save.snapshot()?);
            }
            saves.sort_by(|left, right| left.name.cmp(&right.name));
            Ok(saves)
        })
        .await?
    }

    fn new(
        server: Arc<Server>,
        name: String,
        path: PathBuf,
        configuration: SaveConfiguration,
    ) -> Self {
        Self {
            server,
            name,
            path,
            current_name: configuration.current_name,
            fallback: configuration.settings,
        }
    }

    pub(crate) fn name(&self) -> &str {
        &self.name
    }

    pub(crate) fn path(&self) -> &Path {
        &self.path
    }

    pub(crate) fn is_running(&self) -> bool {
        self.server.basic_config.default_level_name == self.name
    }

    pub(crate) fn is_current(&self) -> bool {
        self.current_name.as_ref() == self.name
    }

    pub(crate) fn snapshot(&self) -> Result<SaveSnapshot, SaveError> {
        let metadata = read_save_metadata(self.path(), self.fallback)?;
        let settings = self.resolved_settings(&metadata);
        Ok(SaveSnapshot {
            name: self.name.clone(),
            display_name: metadata.display_name,
            path: absolute_path_string(self.path())?,
            size: directory_size(self.path())?,
            is_running: self.is_running(),
            is_current: self.is_current(),
            game_mode: settings.game_mode,
            difficulty: settings.difficulty,
            difficulty_locked: settings.difficulty_locked,
            hardcore: settings.hardcore,
            datapacks: metadata.datapacks,
        })
    }

    fn resolved_settings(&self, metadata: &SaveMetadata) -> SaveSettings {
        let runtime = if self.is_running() {
            let level_info = self.server.level_info.load();
            Some((level_info.difficulty, level_info.difficulty_locked))
        } else {
            None
        };
        resolve_save_settings(metadata, runtime)
    }

    fn settings_sync(&self) -> Result<SaveSettings, SaveError> {
        let metadata = read_save_metadata(self.path(), self.fallback)?;
        Ok(self.resolved_settings(&metadata))
    }

    pub(crate) async fn apply_edit(&self, edit: SaveEdit) -> Result<(), SaveError> {
        let is_running = self.is_running();
        if is_running {
            self.server
                .save_all()
                .await
                .map_err(|error| SaveError::Server(error.to_string()))?;
        }

        let path = self.path.clone();
        let file_edit = edit.clone();
        tokio::task::spawn_blocking(move || edit_save_metadata(&path, &file_edit, !is_running))
            .await??;

        if self.is_current() {
            update_save_configuration(None, edit.settings).await?;
        }

        if is_running {
            let level_info = self.server.level_info.load();
            let difficulty_changed = level_info.difficulty != edit.settings.difficulty;
            let difficulty_lock_changed =
                level_info.difficulty_locked != edit.settings.difficulty_locked;
            drop(level_info);

            if difficulty_changed {
                self.server.set_difficulty(edit.settings.difficulty, true);
            }
            if difficulty_lock_changed {
                self.server
                    .set_difficulty_locked(edit.settings.difficulty_locked);
            }
        }
        Ok(())
    }

    pub(crate) async fn set_current(&self) -> Result<(), SaveError> {
        if self.is_current() {
            return Ok(());
        }
        let save = self.clone();
        let settings = tokio::task::spawn_blocking(move || save.settings_sync()).await??;
        update_save_configuration(Some(self.name()), settings).await
    }

    pub(crate) async fn archive_to(&self, target: &Path) -> Result<(), SaveError> {
        if self.is_running() {
            self.server
                .save_all()
                .await
                .map_err(|error| SaveError::Server(error.to_string()))?;
        }
        let source = self.path.clone();
        let target = target.to_path_buf();
        tokio::task::spawn_blocking(move || create_save_archive(&source, &target)).await?
    }

    pub(crate) async fn toggle_datapack(
        &self,
        datapack: String,
        enable: bool,
    ) -> Result<(), SaveError> {
        let path = self.path.clone();
        let metadata_datapack = datapack.clone();
        let changed = tokio::task::spawn_blocking(move || {
            toggle_datapack_metadata(&path, &metadata_datapack, enable)
        })
        .await??;

        if changed && self.is_running() {
            let mut level_info = (**self.server.level_info.load()).clone();
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
            self.server.level_info.store(Arc::new(level_info));
            DatapackManager::reload(&self.server).map_err(SaveError::Datapack)?;
        }
        Ok(())
    }

    pub(crate) async fn delete(self) -> Result<(), SaveError> {
        if self.is_running() || self.is_current() {
            return Err(SaveError::ActiveSave);
        }
        tokio::task::spawn_blocking(move || fs::remove_dir_all(self.path)).await??;
        Ok(())
    }
}

#[derive(Clone)]
struct SaveConfiguration {
    current_name: Arc<str>,
    settings: SaveSettings,
}

impl SaveConfiguration {
    async fn read(server: &Server) -> Result<Self, SaveError> {
        let fallback = SaveSettings::from_server(server);
        let contents = pumpkin_config::read_to_string().await?;
        parse_save_configuration(&contents, &server.basic_config.default_level_name, fallback)
    }
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

fn resolve_save_settings(
    metadata: &SaveMetadata,
    runtime: Option<(Difficulty, bool)>,
) -> SaveSettings {
    let (difficulty, difficulty_locked) =
        runtime.unwrap_or((metadata.difficulty, metadata.difficulty_locked));
    SaveSettings {
        game_mode: metadata.game_mode,
        difficulty,
        difficulty_locked,
        hardcore: metadata.hardcore,
    }
}

fn parse_save_configuration(
    contents: &str,
    running_name: &str,
    fallback: SaveSettings,
) -> Result<SaveConfiguration, SaveError> {
    let document = pumpkin_config::parse(contents)
        .map_err(|error| io::Error::new(io::ErrorKind::InvalidData, error))?;
    let current_name = document
        .get("default_level_name")
        .and_then(Item::as_str)
        .unwrap_or(running_name);
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
    Ok(SaveConfiguration {
        current_name: Arc::from(current_name),
        settings: SaveSettings {
            game_mode,
            difficulty,
            difficulty_locked: fallback.difficulty_locked,
            hardcore,
        },
    })
}

fn validate_save_path(save_name: &str) -> Result<PathBuf, SaveError> {
    let path = PathBuf::from(save_name);
    let valid = fs::symlink_metadata(&path)
        .map(|metadata| metadata.is_dir() && !metadata.file_type().is_symlink())
        .unwrap_or(false)
        && path.join(LEVEL_DATA_FILE).is_file();
    if !valid {
        return Err(SaveError::NotFound);
    }
    Ok(path)
}

fn read_save_metadata(path: &Path, fallback: SaveSettings) -> Result<SaveMetadata, SaveError> {
    let root = read_level_data(path)?;
    let data = root
        .get_compound("Data")
        .ok_or_else(|| SaveError::InvalidLevelData("missing the Data compound".to_string()))?;
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
    edit: &SaveEdit,
    write_difficulty_settings: bool,
) -> Result<(), SaveError> {
    let mut root = read_level_data(path)?;
    let data = data_compound_mut(&mut root)?;
    data.put_string("LevelName", edit.display_name.clone());
    data.put_int("GameType", edit.settings.game_mode as i32);
    data.put_bool("hardcore", edit.settings.hardcore);
    let mut modern = data
        .get_compound("difficulty_settings")
        .cloned()
        .unwrap_or_default();
    if write_difficulty_settings {
        data.put_byte("Difficulty", edit.settings.difficulty as i8);
        data.put_bool("DifficultyLocked", edit.settings.difficulty_locked);
        modern.put_string("difficulty", edit.settings.difficulty.name().to_string());
        modern.put_bool("locked", edit.settings.difficulty_locked);
    }
    modern.put_bool("hardcore", edit.settings.hardcore);
    data.put_compound("difficulty_settings", modern);
    write_level_data(path, root)
}

fn toggle_datapack_metadata(path: &Path, datapack: &str, enable: bool) -> Result<bool, SaveError> {
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

fn read_level_data(path: &Path) -> Result<NbtCompound, SaveError> {
    let file = File::open(path.join(LEVEL_DATA_FILE))?;
    read_gzip_compound_tag(file).map_err(|error| SaveError::InvalidLevelData(error.to_string()))
}

fn write_level_data(path: &Path, root: NbtCompound) -> Result<(), SaveError> {
    let file = File::create(path.join(LEVEL_DATA_FILE))?;
    write_gzip_compound_tag(root, file)
        .map_err(|error| SaveError::InvalidLevelData(error.to_string()))
}

fn data_compound_mut(root: &mut NbtCompound) -> Result<&mut NbtCompound, SaveError> {
    match root.child_tags.get_mut("Data") {
        Some(NbtTag::Compound(data)) => Ok(data),
        _ => Err(SaveError::InvalidLevelData(
            "missing the Data compound".to_string(),
        )),
    }
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

fn directory_size(path: &Path) -> io::Result<u64> {
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

fn create_save_archive(source: &Path, target: &Path) -> Result<(), SaveError> {
    let root_name = source
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or_else(|| SaveError::InvalidLevelData("invalid save directory name".to_string()))?;
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
) -> Result<(), SaveError> {
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
            io::copy(&mut input, writer)?;
        }
    }
    Ok(())
}

async fn update_save_configuration(
    save_name: Option<&str>,
    settings: SaveSettings,
) -> Result<(), SaveError> {
    let contents = pumpkin_config::read_to_string().await?;
    let mut document = pumpkin_config::parse(&contents)
        .map_err(|error| io::Error::new(io::ErrorKind::InvalidData, error))?;
    set_save_configuration(&mut document, save_name, settings);
    pumpkin_config::write(document.to_string()).await?;
    Ok(())
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::utils::file::random_temporary_path;

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
    fn runtime_snapshot_replaces_only_file_difficulty_fields() {
        let metadata = SaveMetadata {
            display_name: "World".to_string(),
            game_mode: GameMode::Creative,
            difficulty: Difficulty::Easy,
            difficulty_locked: false,
            hardcore: true,
            datapacks: BTreeMap::new(),
        };

        let stopped = resolve_save_settings(&metadata, None);
        assert_eq!(stopped.game_mode, GameMode::Creative);
        assert_eq!(stopped.difficulty, Difficulty::Easy);
        assert!(!stopped.difficulty_locked);
        assert!(stopped.hardcore);

        let running = resolve_save_settings(&metadata, Some((Difficulty::Hard, true)));
        assert_eq!(running.game_mode, GameMode::Creative);
        assert_eq!(running.difficulty, Difficulty::Hard);
        assert!(running.difficulty_locked);
        assert!(running.hardcore);
    }

    #[test]
    fn invalid_save_configuration_fails_closed() {
        let error = match parse_save_configuration(
            "default_level_name = [",
            "world",
            SaveSettings {
                game_mode: GameMode::Survival,
                difficulty: Difficulty::Normal,
                difficulty_locked: false,
                hardcore: false,
            },
        ) {
            Ok(_) => panic!("invalid configuration should fail"),
            Err(error) => error,
        };

        assert!(matches!(
            error,
            SaveError::Io(error) if error.kind() == io::ErrorKind::InvalidData
        ));
    }

    #[test]
    fn editing_metadata_preserves_unknown_fields_and_enforces_hardcore_settings() {
        let directory = temporary_directory("save-metadata");
        write_test_level_data(&directory);
        let edit = SaveEdit::new(
            "Renamed".to_string(),
            GameMode::Creative,
            Difficulty::Easy,
            false,
            true,
        );

        edit_save_metadata(&directory, &edit, true).unwrap();

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
        assert!(!directory.join("level.dat_old").exists());
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn editing_running_metadata_preserves_file_difficulty_settings() {
        let directory = temporary_directory("running-save-metadata");
        write_test_level_data(&directory);
        let persisted = SaveEdit::new(
            "Original".to_string(),
            GameMode::Creative,
            Difficulty::Easy,
            false,
            false,
        );
        edit_save_metadata(&directory, &persisted, true).unwrap();
        let runtime = SaveEdit::new(
            "Renamed".to_string(),
            GameMode::Survival,
            Difficulty::Hard,
            true,
            true,
        );

        edit_save_metadata(&directory, &runtime, false).unwrap();

        let root = read_level_data(&directory).unwrap();
        let data = root.get_compound("Data").unwrap();
        assert_eq!(data.get_string("LevelName"), Some("Renamed"));
        assert_eq!(data.get_int("GameType"), Some(0));
        assert_eq!(data.get_byte("Difficulty"), Some(1));
        assert_eq!(data.get_bool("DifficultyLocked"), Some(false));
        assert_eq!(data.get_bool("hardcore"), Some(true));
        let modern = data.get_compound("difficulty_settings").unwrap();
        assert_eq!(modern.get_string("difficulty"), Some("easy"));
        assert_eq!(modern.get_bool("locked"), Some(false));
        assert_eq!(modern.get_bool("hardcore"), Some(true));
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn toggling_a_known_datapack_moves_it_between_lists() {
        let directory = temporary_directory("datapack");
        write_test_level_data(&directory);

        assert!(toggle_datapack_metadata(&directory, "file/example", false).unwrap());
        assert!(!toggle_datapack_metadata(&directory, "file/missing", true).unwrap());

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
    fn archive_skips_session_lock() {
        let parent = temporary_directory("archive-source");
        let source = parent.join("world");
        write_test_level_data(&source);
        fs::write(source.join("notes.txt"), "hello").unwrap();
        fs::write(source.join("session.lock"), "locked").unwrap();
        let archive_path = parent.join("world.zip");

        create_save_archive(&source, &archive_path).unwrap();

        let mut archive = zip::ZipArchive::new(File::open(archive_path).unwrap()).unwrap();
        let entries = (0..archive.len())
            .map(|index| archive.by_index(index).unwrap().name().to_string())
            .collect::<Vec<_>>();
        assert!(entries.contains(&"world/level.dat".to_string()));
        assert!(entries.contains(&"world/notes.txt".to_string()));
        assert!(!entries.contains(&"world/session.lock".to_string()));
        fs::remove_dir_all(parent).unwrap();
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
