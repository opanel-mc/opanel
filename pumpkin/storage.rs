use std::{
    marker::PhantomData,
    path::{Component, Path, PathBuf},
    str,
};

use serde::{Serialize, de::DeserializeOwned};
use serde_json::Value;
use thiserror::Error;
use tokio::{fs, sync::Mutex};
pub(crate) struct Storage {
    root: PathBuf,
    access: Mutex<()>,
}

/// A typed JSON value together with whether loading discovered data that still needs saving.
///
/// Loading never writes the file. A missing file or fields supplied from the default value set
/// `needs_persist`, leaving the caller to decide when the completed value should be committed.
#[derive(Debug)]
pub(crate) struct LoadedJson<T> {
    pub(crate) value: T,
    pub(crate) needs_persist: bool,
}

struct JsonDocument<T> {
    value: T,
    raw: Value,
    needs_persist: bool,
}

#[allow(dead_code)]
pub(crate) struct JsonFile<T> {
    file_name: &'static str,
    default: fn() -> T,
    marker: PhantomData<fn() -> T>,
}

#[allow(dead_code)]
impl<T> JsonFile<T> {
    pub(crate) const fn new(file_name: &'static str, default: fn() -> T) -> Self {
        Self {
            file_name,
            default,
            marker: PhantomData,
        }
    }
}

#[allow(dead_code)]
pub(crate) struct TextFile {
    file_name: &'static str,
    default: &'static str,
}

#[allow(dead_code)]
impl TextFile {
    pub(crate) const fn new(file_name: &'static str, default: &'static str) -> Self {
        Self { file_name, default }
    }
}

#[derive(Debug, Error)]
pub(crate) enum StorageError {
    #[error("invalid storage file name `{file_name}`")]
    InvalidFileName { file_name: &'static str },
    #[error("failed to create storage directory at {path}: {source}")]
    CreateDirectory {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("failed to read storage file at {path}: {source}")]
    Read {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("invalid JSON storage file at {path}: {reason}")]
    InvalidJson { path: PathBuf, reason: String },
    #[error("failed to write storage file at {path}: {source}")]
    Write {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("failed to delete storage file at {path}: {source}")]
    Delete {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("failed to serialize storage file at {path}: {source}")]
    Serialize {
        path: PathBuf,
        #[source]
        source: serde_json::Error,
    },
}

#[allow(dead_code)]
impl Storage {
    pub(crate) async fn open(root: PathBuf) -> Result<Self, StorageError> {
        fs::create_dir_all(&root)
            .await
            .map_err(|source| StorageError::CreateDirectory {
                path: root.clone(),
                source,
            })?;

        Ok(Self {
            root,
            access: Mutex::new(()),
        })
    }

    /// Loads and validates a JSON document without changing it on disk.
    pub(crate) async fn load_json<T>(
        &self,
        file: &JsonFile<T>,
    ) -> Result<LoadedJson<T>, StorageError>
    where
        T: Serialize + DeserializeOwned,
    {
        let _access = self.access.lock().await;
        let document = self.load_json_document_unlocked(file).await?;
        Ok(LoadedJson {
            value: document.value,
            needs_persist: document.needs_persist,
        })
    }

    /// Merges a typed value into the latest JSON document, retaining fields unknown to `T`.
    pub(crate) async fn merge_json<T>(
        &self,
        file: &JsonFile<T>,
        value: &T,
    ) -> Result<(), StorageError>
    where
        T: Serialize + DeserializeOwned,
    {
        let _access = self.access.lock().await;
        let path = self.resolve(file.file_name)?;
        let mut document = self.load_json_document_unlocked(file).await?;
        let update = serialize_json_value(&path, value)?;
        merge_json_values(&mut document.raw, update);
        write_json_value(&path, &document.raw).await
    }

    /// Applies a read-modify-write operation while retaining fields unknown to `T`.
    pub(crate) async fn update_json<T, R>(
        &self,
        file: &JsonFile<T>,
        update: impl FnOnce(&mut T) -> R,
    ) -> Result<R, StorageError>
    where
        T: Serialize + DeserializeOwned,
    {
        let _access = self.access.lock().await;
        let path = self.resolve(file.file_name)?;
        let mut document = self.load_json_document_unlocked(file).await?;
        let result = update(&mut document.value);
        let update = serialize_json_value(&path, &document.value)?;
        merge_json_values(&mut document.raw, update);
        write_json_value(&path, &document.raw).await?;
        Ok(result)
    }

    pub(crate) async fn read_text(&self, file: &TextFile) -> Result<String, StorageError> {
        let _access = self.access.lock().await;
        self.read_text_unlocked(file).await
    }

    pub(crate) async fn write_text(
        &self,
        file: &TextFile,
        value: &str,
    ) -> Result<(), StorageError> {
        let _access = self.access.lock().await;
        let path = self.resolve(file.file_name)?;
        write_bytes(&path, value.as_bytes()).await
    }

    pub(crate) async fn delete_text(&self, file: &TextFile) -> Result<(), StorageError> {
        let _access = self.access.lock().await;
        let path = self.resolve(file.file_name)?;
        match fs::remove_file(&path).await {
            Ok(()) => Ok(()),
            Err(source) if source.kind() == std::io::ErrorKind::NotFound => Ok(()),
            Err(source) => Err(StorageError::Delete { path, source }),
        }
    }

    pub(crate) async fn update_text<R>(
        &self,
        file: &TextFile,
        update: impl FnOnce(&mut String) -> R,
    ) -> Result<R, StorageError> {
        let _access = self.access.lock().await;
        let mut value = self.read_text_unlocked(file).await?;
        let result = update(&mut value);
        let path = self.resolve(file.file_name)?;
        write_bytes(&path, value.as_bytes()).await?;
        Ok(result)
    }

    async fn load_json_document_unlocked<T>(
        &self,
        file: &JsonFile<T>,
    ) -> Result<JsonDocument<T>, StorageError>
    where
        T: Serialize + DeserializeOwned,
    {
        let path = self.resolve(file.file_name)?;
        let default = (file.default)();
        let default_tree = serialize_json_value(&path, &default)?;

        let bytes = match fs::read(&path).await {
            Ok(bytes) => bytes,
            Err(source) if source.kind() == std::io::ErrorKind::NotFound => {
                return Ok(JsonDocument {
                    value: default,
                    raw: default_tree,
                    needs_persist: true,
                });
            }
            Err(source) => {
                return Err(StorageError::Read { path, source });
            }
        };

        let raw_text = str::from_utf8(&bytes).map_err(|error| StorageError::InvalidJson {
            path: path.clone(),
            reason: error.to_string(),
        })?;
        let mut json =
            serde_json::from_str::<Value>(raw_text).map_err(|error| StorageError::InvalidJson {
                path: path.clone(),
                reason: error.to_string(),
            })?;
        if json.is_null() {
            return Err(StorageError::InvalidJson {
                path,
                reason: "the JSON root is null".to_string(),
            });
        }

        let changed = fill_missing_values(&mut json, &default_tree);
        let value = serde_json::from_value::<T>(json.clone()).map_err(|error| {
            StorageError::InvalidJson {
                path,
                reason: error.to_string(),
            }
        })?;

        Ok(JsonDocument {
            value,
            raw: json,
            needs_persist: changed,
        })
    }

    async fn read_text_unlocked(&self, file: &TextFile) -> Result<String, StorageError> {
        let path = self.resolve(file.file_name)?;
        match fs::read_to_string(&path).await {
            Ok(value) => Ok(value),
            Err(source) if source.kind() == std::io::ErrorKind::NotFound => {
                write_bytes(&path, file.default.as_bytes()).await?;
                Ok(file.default.to_string())
            }
            Err(source) => Err(StorageError::Read { path, source }),
        }
    }

    fn resolve(&self, file_name: &'static str) -> Result<PathBuf, StorageError> {
        let path = Path::new(file_name);
        let mut components = path.components();
        let is_single_normal_component =
            matches!(components.next(), Some(Component::Normal(_))) && components.next().is_none();
        if file_name.is_empty()
            || file_name.contains(['/', '\\', '\0'])
            || !is_single_normal_component
        {
            return Err(StorageError::InvalidFileName { file_name });
        }

        Ok(self.root.join(path))
    }
}

async fn write_json_value<T>(path: &Path, value: &T) -> Result<(), StorageError>
where
    T: Serialize + ?Sized,
{
    let json = serde_json::to_string_pretty(value).map_err(|source| StorageError::Serialize {
        path: path.to_path_buf(),
        source,
    })?;
    write_bytes(path, json.as_bytes()).await
}

fn serialize_json_value<T>(path: &Path, value: &T) -> Result<Value, StorageError>
where
    T: Serialize + ?Sized,
{
    serde_json::to_value(value).map_err(|source| StorageError::Serialize {
        path: path.to_path_buf(),
        source,
    })
}

async fn write_bytes(path: &Path, value: &[u8]) -> Result<(), StorageError> {
    fs::write(path, value)
        .await
        .map_err(|source| StorageError::Write {
            path: path.to_path_buf(),
            source,
        })
}

fn fill_missing_values(target: &mut Value, defaults: &Value) -> bool {
    let (Some(target), Some(defaults)) = (target.as_object_mut(), defaults.as_object()) else {
        return false;
    };

    let mut changed = false;
    for (key, default_value) in defaults {
        match target.get_mut(key) {
            Some(target_value) => {
                changed |= fill_missing_values(target_value, default_value);
            }
            None => {
                target.insert(key.clone(), default_value.clone());
                changed = true;
            }
        }
    }
    changed
}

fn merge_json_values(target: &mut Value, update: Value) {
    match update {
        Value::Object(update) => {
            let Value::Object(target) = target else {
                *target = Value::Object(update);
                return;
            };
            for (key, update_value) in update {
                match target.get_mut(&key) {
                    Some(target_value) => merge_json_values(target_value, update_value),
                    None => {
                        target.insert(key, update_value);
                    }
                }
            }
        }
        update => *target = update,
    }
}

#[cfg(test)]
mod tests {
    use std::{
        path::PathBuf,
        sync::{
            Arc,
            atomic::{AtomicU64, Ordering},
        },
    };

    use serde::{Deserialize, Serialize};
    use serde_json::json;

    use super::{JsonFile, Storage, StorageError, TextFile};

    static NEXT_TEST_DIRECTORY: AtomicU64 = AtomicU64::new(0);

    #[derive(Debug, Deserialize, PartialEq, Serialize)]
    #[serde(rename_all = "camelCase")]
    struct TestSettings {
        enabled: bool,
        nested: NestedSettings,
    }

    #[derive(Debug, Deserialize, PartialEq, Serialize)]
    #[serde(rename_all = "camelCase")]
    struct NestedSettings {
        name: String,
        retry_count: u32,
    }

    fn default_settings() -> TestSettings {
        TestSettings {
            enabled: false,
            nested: NestedSettings {
                name: "default".to_string(),
                retry_count: 3,
            },
        }
    }

    const SETTINGS: JsonFile<TestSettings> = JsonFile::new("settings.json", default_settings);
    const NOTES: TextFile = TextFile::new("notes.txt", "default notes");

    #[derive(Debug, Deserialize, PartialEq, Serialize)]
    struct Counter {
        value: u32,
    }

    fn default_counter() -> Counter {
        Counter { value: 0 }
    }

    const COUNTER: JsonFile<Counter> = JsonFile::new("counter.json", default_counter);

    struct TestDirectory {
        path: PathBuf,
    }

    impl TestDirectory {
        fn new() -> Self {
            let sequence = NEXT_TEST_DIRECTORY.fetch_add(1, Ordering::Relaxed);
            let path = std::env::temp_dir().join(format!(
                "opanel-storage-test-{}-{sequence}",
                std::process::id()
            ));
            Self { path }
        }

        fn child(&self, name: &str) -> PathBuf {
            self.path.join(name)
        }
    }

    impl Drop for TestDirectory {
        fn drop(&mut self) {
            let temp_dir = std::env::temp_dir();
            let is_test_directory = self
                .path
                .file_name()
                .and_then(|name| name.to_str())
                .is_some_and(|name| name.starts_with("opanel-storage-test-"));
            if self.path.starts_with(temp_dir) && is_test_directory {
                let _ = std::fs::remove_dir_all(&self.path);
            }
        }
    }

    async fn test_storage() -> (TestDirectory, Storage) {
        let directory = TestDirectory::new();
        let storage = Storage::open(directory.child("nested/opanel"))
            .await
            .expect("test storage should open");
        (directory, storage)
    }

    #[tokio::test]
    async fn creates_root_without_persisting_missing_json_defaults() {
        let (directory, storage) = test_storage().await;
        assert!(directory.child("nested/opanel").is_dir());

        let loaded = storage.load_json(&SETTINGS).await.unwrap();
        assert_eq!(loaded.value, default_settings());
        assert!(loaded.needs_persist);
        assert_eq!(storage.read_text(&NOTES).await.unwrap(), "default notes");
        assert!(!directory.child("nested/opanel/settings.json").exists());
        assert!(directory.child("nested/opanel/notes.txt").is_file());
    }

    #[tokio::test]
    async fn merges_and_updates_json_and_text() {
        let (directory, storage) = test_storage().await;
        let settings = TestSettings {
            enabled: true,
            nested: NestedSettings {
                name: "custom".to_string(),
                retry_count: 8,
            },
        };
        storage.merge_json(&SETTINGS, &settings).await.unwrap();
        let loaded = storage.load_json(&SETTINGS).await.unwrap();
        assert_eq!(loaded.value, settings);
        assert!(!loaded.needs_persist);

        storage
            .update_json(&SETTINGS, |settings| {
                settings.nested.retry_count += 1;
            })
            .await
            .unwrap();
        assert_eq!(
            storage
                .load_json(&SETTINGS)
                .await
                .unwrap()
                .value
                .nested
                .retry_count,
            9
        );

        storage.write_text(&NOTES, "first").await.unwrap();
        storage
            .update_text(&NOTES, |notes| notes.push_str(" second"))
            .await
            .unwrap();
        assert_eq!(storage.read_text(&NOTES).await.unwrap(), "first second");

        tokio::fs::write(directory.child("nested/opanel/notes.txt"), "external")
            .await
            .unwrap();
        assert_eq!(storage.read_text(&NOTES).await.unwrap(), "external");

        storage.delete_text(&NOTES).await.unwrap();
        assert!(!directory.child("nested/opanel/notes.txt").exists());
        storage.delete_text(&NOTES).await.unwrap();
    }

    #[tokio::test]
    async fn load_fills_missing_fields_without_writing_and_merge_preserves_unknown_fields() {
        let (directory, storage) = test_storage().await;
        let path = directory.child("nested/opanel/settings.json");
        let original =
            r#"{"enabled":true,"nested":{"name":"custom","futureNested":true},"unknown":42}"#;
        tokio::fs::write(&path, original).await.unwrap();

        let loaded = storage.load_json(&SETTINGS).await.unwrap();
        assert_eq!(
            loaded.value,
            TestSettings {
                enabled: true,
                nested: NestedSettings {
                    name: "custom".to_string(),
                    retry_count: 3,
                },
            }
        );
        assert!(loaded.needs_persist);
        assert_eq!(tokio::fs::read_to_string(&path).await.unwrap(), original);

        storage.merge_json(&SETTINGS, &loaded.value).await.unwrap();

        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(path).await.unwrap()).unwrap();
        assert_eq!(stored["unknown"], 42);
        assert_eq!(stored["nested"]["futureNested"], true);
        assert_eq!(stored["nested"]["retryCount"], 3);
    }

    #[tokio::test]
    async fn invalid_json_returns_an_error_without_overwriting() {
        let (directory, storage) = test_storage().await;
        let path = directory.child("nested/opanel/settings.json");
        let invalid_values: &[&[u8]] = &[
            b"{".as_slice(),
            b"null".as_slice(),
            br#"{"enabled":"yes","nested":{}}"#.as_slice(),
            &[0xff],
        ];

        for invalid in invalid_values {
            tokio::fs::write(&path, invalid).await.unwrap();
            let error = storage.load_json(&SETTINGS).await.unwrap_err();
            match error {
                StorageError::InvalidJson {
                    path: error_path,
                    reason,
                } => {
                    assert_eq!(error_path, path);
                    assert!(!reason.is_empty());
                }
                error => panic!("expected invalid JSON error, got {error:?}"),
            }
            assert_eq!(tokio::fs::read(&path).await.unwrap(), *invalid);
        }
    }

    #[tokio::test]
    async fn merge_and_update_reject_invalid_json_without_overwriting() {
        let (directory, storage) = test_storage().await;
        let path = directory.child("nested/opanel/settings.json");
        let invalid = br#"{"enabled":"yes","nested":{}}"#;
        tokio::fs::write(&path, invalid).await.unwrap();

        assert!(matches!(
            storage.merge_json(&SETTINGS, &default_settings()).await,
            Err(StorageError::InvalidJson { .. })
        ));
        assert_eq!(tokio::fs::read(&path).await.unwrap(), invalid);

        assert!(matches!(
            storage.update_json(&SETTINGS, |_| ()).await,
            Err(StorageError::InvalidJson { .. })
        ));
        assert_eq!(tokio::fs::read(path).await.unwrap(), invalid);
    }

    #[tokio::test]
    async fn update_json_preserves_unknown_fields() {
        let (directory, storage) = test_storage().await;
        let path = directory.child("nested/opanel/counter.json");
        tokio::fs::write(&path, r#"{"value":4,"future":{"enabled":true}}"#)
            .await
            .unwrap();

        storage
            .update_json(&COUNTER, |counter| counter.value += 1)
            .await
            .unwrap();

        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(path).await.unwrap()).unwrap();
        assert_eq!(stored["value"], 5);
        assert_eq!(stored["future"]["enabled"], true);
    }

    #[test]
    fn recursively_merges_objects_and_replaces_arrays_and_scalars() {
        let mut target = json!({
            "scalar": 1,
            "array": [1, 2],
            "nested": {
                "known": "old",
                "unknown": true
            },
            "topLevelUnknown": 42
        });

        super::merge_json_values(
            &mut target,
            json!({
                "scalar": 2,
                "array": [3],
                "nested": {
                    "known": "new"
                }
            }),
        );

        assert_eq!(
            target,
            json!({
                "scalar": 2,
                "array": [3],
                "nested": {
                    "known": "new",
                    "unknown": true
                },
                "topLevelUnknown": 42
            })
        );
    }

    #[tokio::test]
    async fn invalid_utf8_text_returns_an_error_without_overwriting() {
        let (directory, storage) = test_storage().await;
        let path = directory.child("nested/opanel/notes.txt");
        tokio::fs::write(&path, [0xff]).await.unwrap();

        assert!(matches!(
            storage.read_text(&NOTES).await,
            Err(StorageError::Read { .. })
        ));
        assert_eq!(tokio::fs::read(path).await.unwrap(), [0xff]);
    }

    #[tokio::test]
    async fn rejects_unsafe_file_names() {
        let (_directory, storage) = test_storage().await;
        for file_name in [
            "",
            ".",
            "..",
            "/absolute",
            "../secret",
            "nested/file",
            "nested\\file",
        ] {
            let file = TextFile::new(file_name, "");
            assert!(matches!(
                storage.read_text(&file).await,
                Err(StorageError::InvalidFileName { .. })
            ));
        }
    }

    #[tokio::test]
    async fn serializes_concurrent_updates() {
        let (directory, storage) = test_storage().await;
        let storage = Arc::new(storage);
        let mut updates = Vec::new();
        for _ in 0..50 {
            let storage = Arc::clone(&storage);
            updates.push(tokio::spawn(async move {
                storage
                    .update_json(&COUNTER, |counter| counter.value += 1)
                    .await
                    .unwrap();
            }));
        }
        for update in updates {
            update.await.unwrap();
        }

        assert_eq!(storage.load_json(&COUNTER).await.unwrap().value.value, 50);
        assert!(directory.child("nested/opanel/counter.json").is_file());
    }

    #[test]
    fn storage_is_send_and_sync() {
        fn assert_send_sync<T: Send + Sync>() {}
        assert_send_sync::<Storage>();
    }
}
