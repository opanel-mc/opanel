use std::{
    fs,
    io::{self, BufRead, BufReader, Read, Seek, SeekFrom},
    path::{Path, PathBuf},
    time::SystemTime,
};

use flate2::read::MultiGzDecoder;
use thiserror::Error;

use super::file::is_safe_file_name;

#[derive(Debug, Error)]
pub(crate) enum LogError {
    #[error("Illegal file name or file extension.")]
    InvalidName,
    #[error("You cannot delete latest.log or debug.log.")]
    ActiveLog,
    #[error(transparent)]
    Io(#[from] io::Error),
}

pub(crate) fn list(directory: &Path) -> Result<Vec<String>, LogError> {
    fs::create_dir_all(directory)?;
    let mut files = Vec::new();
    for entry in fs::read_dir(directory)? {
        let entry = entry?;
        if entry.file_type()?.is_file() {
            files.push(entry.file_name().to_string_lossy().into_owned());
        }
    }
    files.sort();
    Ok(files)
}

fn path(directory: &Path, name: &str) -> Result<PathBuf, LogError> {
    if !is_safe_file_name(name) || name.contains(':') {
        return Err(LogError::InvalidName);
    }
    Ok(directory.join(name))
}

pub(crate) fn read(directory: &Path, name: &str) -> Result<String, LogError> {
    let path = path(directory, name)?;
    let mut file = fs::File::open(path)?;
    let mut bytes = Vec::new();
    if name.ends_with(".gz") {
        MultiGzDecoder::new(file).read_to_end(&mut bytes)?;
    } else if name.ends_with(".log") || name.ends_with(".txt") {
        file.read_to_end(&mut bytes)?;
    } else {
        return Err(LogError::InvalidName);
    }
    Ok(String::from_utf8_lossy(&bytes).into_owned())
}

pub(crate) fn delete(directory: &Path, name: &str) -> Result<(), LogError> {
    let path = path(directory, name)?;
    if name.ends_with(".log") {
        return Err(LogError::ActiveLog);
    }
    fs::remove_file(path)?;
    Ok(())
}

pub(crate) fn clear(directory: &Path) -> Result<(), LogError> {
    for name in list(directory)? {
        if name.ends_with(".log.gz") {
            delete(directory, &name)?;
        }
    }
    Ok(())
}

/// Reads complete lines appended after the listener starts.
pub(crate) struct LogTail {
    path: PathBuf,
    offset: u64,
    created: Option<SystemTime>,
    pending: Vec<u8>,
}

impl LogTail {
    pub(crate) fn new(path: PathBuf) -> io::Result<Self> {
        let metadata = match fs::metadata(&path) {
            Ok(metadata) => Some(metadata),
            Err(error) if error.kind() == io::ErrorKind::NotFound => None,
            Err(error) => return Err(error),
        };
        Ok(Self {
            path,
            offset: metadata.as_ref().map_or(0, fs::Metadata::len),
            created: metadata.and_then(|metadata| metadata.created().ok()),
            pending: Vec::new(),
        })
    }

    pub(crate) fn read_lines(&mut self) -> io::Result<Vec<String>> {
        let mut file = match fs::File::open(&self.path) {
            Ok(file) => file,
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                self.offset = 0;
                self.created = None;
                self.pending.clear();
                return Ok(Vec::new());
            }
            Err(error) => return Err(error),
        };
        let metadata = file.metadata()?;
        let created = metadata.created().ok();
        if metadata.len() < self.offset || created != self.created {
            self.offset = 0;
            self.pending.clear();
        }
        self.created = created;
        file.seek(SeekFrom::Start(self.offset))?;
        let mut reader = BufReader::new(file);
        let mut lines = Vec::new();
        // Bound each batch so a burst of logs does not monopolize the worker.
        while lines.len() < 20_000 {
            let read = reader.read_until(b'\n', &mut self.pending)?;
            self.offset += read as u64;
            if read == 0 {
                break;
            }
            if self.pending.last() == Some(&b'\n') {
                self.pending.pop();
                if self.pending.last() == Some(&b'\r') {
                    self.pending.pop();
                }
                lines.push(String::from_utf8_lossy(&self.pending).into_owned());
                self.pending.clear();
            }
        }
        Ok(lines)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::utils::file::random_temporary_path;
    use std::io::Write;

    #[test]
    fn tail_waits_for_complete_utf8_lines_and_does_not_replay_existing_logs() {
        let path = random_temporary_path(&std::env::temp_dir(), "log").unwrap();
        fs::write(&path, b"[INFO] old\n").unwrap();
        let mut tail = LogTail::new(path.clone()).unwrap();
        assert!(tail.read_lines().unwrap().is_empty());

        let mut writer = fs::OpenOptions::new().append(true).open(&path).unwrap();
        writer.write_all(b"[INFO] \xe4").unwrap();
        assert!(tail.read_lines().unwrap().is_empty());
        writer.write_all(b"\xb8\xad\r\n[WARN] next\n").unwrap();
        assert_eq!(tail.read_lines().unwrap(), ["[INFO] 中", "[WARN] next"]);
        assert!(tail.read_lines().unwrap().is_empty());
        drop(writer);
        fs::remove_file(path).unwrap();
    }

    #[test]
    fn tail_handles_delayed_creation_truncation_and_missing_files() {
        let path = random_temporary_path(&std::env::temp_dir(), "log").unwrap();
        let mut tail = LogTail::new(path.clone()).unwrap();
        assert!(tail.read_lines().unwrap().is_empty());
        fs::write(&path, "[INFO] first log\nunfinished").unwrap();
        assert_eq!(tail.read_lines().unwrap(), ["[INFO] first log"]);
        fs::write(&path, "[WARN] new\n").unwrap();
        assert_eq!(tail.read_lines().unwrap(), ["[WARN] new"]);
        fs::remove_file(&path).unwrap();
        assert!(tail.read_lines().unwrap().is_empty());
        fs::write(&path, "[ERROR] recreated\n").unwrap();
        assert_eq!(tail.read_lines().unwrap(), ["[ERROR] recreated"]);
        fs::remove_file(path).unwrap();
    }

    #[test]
    fn reads_archives_and_clears_only_archived_logs() {
        let root = random_temporary_path(&std::env::temp_dir(), "logs").unwrap();
        assert!(list(&root).unwrap().is_empty());
        fs::write(root.join("latest.log"), "live\n日志\n").unwrap();
        fs::write(root.join("notes.txt"), "notes").unwrap();
        fs::create_dir(root.join("directory")).unwrap();
        let mut encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::default());
        encoder.write_all(b"archived\n").unwrap();
        fs::write(root.join("old.log.gz"), encoder.finish().unwrap()).unwrap();
        assert_eq!(
            list(&root).unwrap(),
            ["latest.log", "notes.txt", "old.log.gz"]
        );
        assert_eq!(read(&root, "latest.log").unwrap(), "live\n日志\n");
        assert_eq!(read(&root, "old.log.gz").unwrap(), "archived\n");
        assert!(matches!(
            delete(&root, "latest.log"),
            Err(LogError::ActiveLog)
        ));
        clear(&root).unwrap();
        assert_eq!(list(&root).unwrap(), ["latest.log", "notes.txt"]);
        delete(&root, "notes.txt").unwrap();
        assert!(
            matches!(read(&root, "missing.log"), Err(LogError::Io(error)) if error.kind() == io::ErrorKind::NotFound)
        );
        for name in [
            "../secret.log",
            "a/b.log",
            "a\\b.log",
            "file:stream.log",
            "",
        ] {
            assert!(matches!(read(&root, name), Err(LogError::InvalidName)));
            assert!(matches!(delete(&root, name), Err(LogError::InvalidName)));
        }
        fs::write(root.join("bad.log.gz"), b"broken").unwrap();
        assert!(read(&root, "bad.log.gz").is_err());
        fs::remove_dir_all(root).unwrap();
    }
}
