use std::{
    fs::{self, File},
    io,
    path::Path,
};

use pumpkin_nbt::{
    compound::NbtCompound,
    nbt_compress::{read_gzip_compound_tag, write_gzip_compound_tag},
};
use pumpkin_util::GameMode;
use uuid::Uuid;

pub(crate) fn list(directory: &Path) -> io::Result<Vec<Uuid>> {
    let entries = match fs::read_dir(directory) {
        Ok(entries) => entries,
        Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok(Vec::new()),
        Err(error) => return Err(error),
    };
    let mut ids = Vec::new();
    for entry in entries {
        let entry = entry?;
        if entry.file_type()?.is_file()
            && let Some(id) = entry
                .file_name()
                .to_str()
                .and_then(|name| name.strip_suffix(".dat"))
                .and_then(|name| Uuid::parse_str(name).ok())
        {
            ids.push(id);
        }
    }
    ids.sort_unstable();
    Ok(ids)
}

pub(crate) fn read(directory: &Path, uuid: Uuid) -> io::Result<NbtCompound> {
    read_gzip_compound_tag(File::open(directory.join(format!("{uuid}.dat")))?)
        .map_err(io::Error::other)
}

pub(crate) fn set_game_mode(directory: &Path, uuid: Uuid, mode: GameMode) -> io::Result<()> {
    let mut data = read(directory, uuid)?;
    data.put_int("playerGameType", mode as i32);
    write(directory, uuid, data)
}

pub(crate) fn write(directory: &Path, uuid: Uuid, data: NbtCompound) -> io::Result<()> {
    write_gzip_compound_tag(data, File::create(directory.join(format!("{uuid}.dat")))?)
        .map_err(io::Error::other)
}

pub(crate) fn delete(directory: &Path, uuid: Uuid) -> io::Result<()> {
    // Keep the primary file discoverable if deleting the backup fails, so the API can retry.
    for extension in ["dat_old", "dat"] {
        match fs::remove_file(directory.join(format!("{uuid}.{extension}"))) {
            Ok(()) => {}
            Err(error) if error.kind() == io::ErrorKind::NotFound => {}
            Err(error) => return Err(error),
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::utils::file::random_temporary_path;

    #[test]
    fn failed_backup_deletion_keeps_player_data_for_retry() {
        let root = random_temporary_path(&std::env::temp_dir(), "players").unwrap();
        fs::create_dir_all(&root).unwrap();
        let id = Uuid::from_u128(1);
        let primary = root.join(format!("{id}.dat"));
        let backup = root.join(format!("{id}.dat_old"));
        fs::write(&primary, b"player data").unwrap();
        // A directory makes remove_file fail on every platform without relying on file locks.
        fs::create_dir(&backup).unwrap();

        assert!(delete(&root, id).is_err());
        assert_eq!(fs::read(&primary).unwrap(), b"player data");
        assert_eq!(list(&root).unwrap(), [id]);

        fs::remove_dir(&backup).unwrap();
        fs::write(&backup, b"backup").unwrap();
        delete(&root, id).unwrap();
        assert!(!primary.exists());
        assert!(!backup.exists());
        fs::remove_dir(root).unwrap();
    }

    #[test]
    fn offline_game_mode_preserves_data_and_deletion_removes_backup() {
        let root = random_temporary_path(&std::env::temp_dir(), "players").unwrap();
        assert!(list(&root).unwrap().is_empty());
        fs::create_dir_all(&root).unwrap();
        let id = Uuid::from_u128(1);
        let other = Uuid::from_u128(2);
        let mut data = NbtCompound::new();
        data.put_int("playerGameType", 0);
        data.put_int("XpLevel", 12);
        write_gzip_compound_tag(data, File::create(root.join(format!("{id}.dat"))).unwrap())
            .unwrap();
        fs::write(root.join(format!("{id}.dat_old")), b"backup").unwrap();
        fs::write(root.join(format!("{other}.dat")), b"other").unwrap();
        fs::write(root.join("invalid.dat"), b"invalid").unwrap();
        assert_eq!(list(&root).unwrap(), [id, other]);
        set_game_mode(&root, id, GameMode::Creative).unwrap();
        let data = read(&root, id).unwrap();
        assert_eq!(data.get_int("playerGameType"), Some(1));
        assert_eq!(data.get_int("XpLevel"), Some(12));
        delete(&root, id).unwrap();
        delete(&root, id).unwrap();
        assert_eq!(list(&root).unwrap(), [other]);
        assert!(!root.join(format!("{id}.dat_old")).exists());
        fs::remove_dir_all(root).unwrap();
    }
}
