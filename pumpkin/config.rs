use std::{
    error::Error,
    future::Future,
    pin::Pin,
    sync::{
        Arc,
        atomic::{AtomicBool, Ordering},
    },
};

use arc_swap::ArcSwap;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use tokio::sync::Mutex;
use tracing::warn;

use crate::{
    managers::{Manager, ManagerContext, OPanelUnavailable},
    storage::{JsonFile, Storage, StorageError, TextFile},
};

const RANDOM_CHARACTERS: &[u8; 64] =
    b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$";
const INITIAL_ACCESS_KEY_TEMPLATE: &str = "# Remember to DELETE this file for your server security!\n\
# 为了您服务器的安全，请记得删除此文件！\n\n";

const CONFIG_FILE: JsonFile<OPanelConfig> = JsonFile::new("config.json", OPanelConfig::default);
const INITIAL_ACCESS_KEY_FILE: TextFile = TextFile::new("INITIAL_ACCESS_KEY.txt", "");

#[derive(Debug, Clone, Deserialize, PartialEq, Eq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct OPanelConfig {
    pub host: String,
    pub port: u16,
    pub access_key: String,
    pub salt: String,
    pub cookie_secure: bool,
    pub proxy_headers: bool,
}

impl Default for OPanelConfig {
    fn default() -> Self {
        Self {
            host: "0.0.0.0".to_string(),
            port: 3000,
            access_key: String::new(),
            salt: String::new(),
            cookie_secure: false,
            proxy_headers: false,
        }
    }
}

#[derive(Debug, Error)]
pub(crate) enum ConfigManagerError {
    #[error(transparent)]
    OPanelUnavailable(#[from] OPanelUnavailable),
    #[error(transparent)]
    Storage(#[from] StorageError),
    #[error("failed to obtain secure random bytes: {0}")]
    Entropy(getrandom::Error),
}

impl From<getrandom::Error> for ConfigManagerError {
    fn from(error: getrandom::Error) -> Self {
        Self::Entropy(error)
    }
}

pub struct ConfigManager {
    context: ManagerContext,
    config: ArcSwap<OPanelConfig>,
    replace_access: Mutex<()>,
    initial_access_key_notice: AtomicBool,
}

impl ConfigManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            config: ArcSwap::from_pointee(OPanelConfig::default()),
            replace_access: Mutex::new(()),
            initial_access_key_notice: AtomicBool::new(false),
        }
    }

    pub fn get(&self) -> Arc<OPanelConfig> {
        self.config.load_full()
    }

    /// Persists the known configuration fields before publishing its in-memory snapshot.
    #[allow(dead_code)]
    pub(crate) async fn replace(&self, config: OPanelConfig) -> Result<(), ConfigManagerError> {
        let storage = self.opanel()?.storage();
        self.replace_in_storage(storage.as_ref(), config).await
    }

    /// Returns the initial-key notice exactly once after a successful first-time setup.
    ///
    /// The web server should consume this only after it has successfully bound its listener.
    pub(crate) fn take_initial_access_key_notice(&self) -> bool {
        self.initial_access_key_notice.swap(false, Ordering::AcqRel)
    }

    async fn initialize(&self, storage: &Storage) -> Result<(), ConfigManagerError> {
        let _replace_access = self.replace_access.lock().await;

        // A plaintext key from a prior successful launch must never survive a restart.
        storage.delete_text(&INITIAL_ACCESS_KEY_FILE).await?;

        let loaded = storage.load_json(&CONFIG_FILE).await?;
        let mut config = loaded.value;
        let mut needs_persist = loaded.needs_persist;
        let mut plaintext_access_key = None;

        if config.access_key.trim().is_empty() {
            let access_key = secure_random_string(12)?;
            config.access_key = md5_hex(&md5_hex(&access_key));
            plaintext_access_key = Some(access_key);
            needs_persist = true;
        }
        if config.salt.trim().is_empty() {
            config.salt = secure_random_string(6)?;
            needs_persist = true;
        }

        if let Some(access_key) = plaintext_access_key.as_deref()
            && let Err(error) = storage
                .write_text(
                    &INITIAL_ACCESS_KEY_FILE,
                    &format!("{INITIAL_ACCESS_KEY_TEMPLATE}{access_key}"),
                )
                .await
        {
            // A failed write may still have created or truncated the file. Cleanup is
            // best-effort and must not hide the original persistence failure.
            cleanup_initial_access_key(storage).await;
            return Err(error.into());
        }

        if needs_persist && let Err(error) = storage.merge_json(&CONFIG_FILE, &config).await {
            if plaintext_access_key.is_some() {
                cleanup_initial_access_key(storage).await;
            }
            return Err(error.into());
        }

        self.config.store(Arc::new(config));
        self.initial_access_key_notice
            .store(plaintext_access_key.is_some(), Ordering::Release);
        Ok(())
    }

    async fn replace_in_storage(
        &self,
        storage: &Storage,
        config: OPanelConfig,
    ) -> Result<(), ConfigManagerError> {
        let _replace_access = self.replace_access.lock().await;
        storage.merge_json(&CONFIG_FILE, &config).await?;
        self.config.store(Arc::new(config));
        Ok(())
    }
}

impl Manager for ConfigManager {
    fn name(&self) -> &'static str {
        "config"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }

    fn start(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            let storage = self.opanel()?.storage();
            self.initialize(storage.as_ref())
                .await
                .map_err(|error| Box::new(error) as Box<dyn Error + Send + Sync>)
        })
    }
}

async fn cleanup_initial_access_key(storage: &Storage) {
    if let Err(error) = storage.delete_text(&INITIAL_ACCESS_KEY_FILE).await {
        warn!(
            %error,
            "Failed to clean up the initial access key after configuration persistence failed"
        );
    }
}

fn secure_random_string(length: usize) -> Result<String, getrandom::Error> {
    let mut bytes = vec![0_u8; length];
    getrandom::fill(&mut bytes)?;

    Ok(bytes
        .into_iter()
        .map(|byte| RANDOM_CHARACTERS[usize::from(byte & 63)] as char)
        .collect())
}

fn md5_hex(value: &str) -> String {
    format!("{:x}", md5::compute(value.as_bytes()))
}

#[cfg(test)]
mod tests {
    use std::{
        path::{Path, PathBuf},
        sync::{
            Arc, Weak,
            atomic::{AtomicU64, Ordering},
        },
    };

    use serde_json::json;
    use tokio_util::sync::CancellationToken;

    use super::{
        CONFIG_FILE, ConfigManager, INITIAL_ACCESS_KEY_FILE, INITIAL_ACCESS_KEY_TEMPLATE,
        OPanelConfig, RANDOM_CHARACTERS, md5_hex, secure_random_string,
    };
    use crate::{
        managers::ManagerContext,
        storage::{Storage, StorageError},
    };

    static NEXT_TEST_DIRECTORY: AtomicU64 = AtomicU64::new(0);

    struct TestDirectory {
        path: PathBuf,
    }

    impl TestDirectory {
        fn new() -> Self {
            let sequence = NEXT_TEST_DIRECTORY.fetch_add(1, Ordering::Relaxed);
            Self {
                path: std::env::temp_dir().join(format!(
                    "opanel-config-test-{}-{sequence}",
                    std::process::id()
                )),
            }
        }

        fn path(&self) -> &Path {
            &self.path
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
                .is_some_and(|name| name.starts_with("opanel-config-test-"));
            if self.path.starts_with(temp_dir) && is_test_directory {
                if self.path.is_dir() {
                    let _ = std::fs::remove_dir_all(&self.path);
                } else {
                    let _ = std::fs::remove_file(&self.path);
                }
            }
        }
    }

    fn manager_context() -> ManagerContext {
        ManagerContext::new(Weak::new(), CancellationToken::new())
    }

    async fn test_storage() -> (TestDirectory, Storage) {
        let directory = TestDirectory::new();
        let storage = Storage::open(directory.path().to_path_buf())
            .await
            .expect("test storage should open");
        (directory, storage)
    }

    #[test]
    fn default_config_matches_the_web_server_defaults() {
        let config = OPanelConfig::default();

        assert_eq!(config.host, "0.0.0.0");
        assert_eq!(config.port, 3000);
        assert!(config.access_key.is_empty());
        assert!(config.salt.is_empty());
        assert!(!config.cookie_secure);
        assert!(!config.proxy_headers);
        assert_eq!(
            serde_json::to_value(config).unwrap(),
            json!({
                "host": "0.0.0.0",
                "port": 3000,
                "accessKey": "",
                "salt": "",
                "cookieSecure": false,
                "proxyHeaders": false,
            })
        );
    }

    #[test]
    fn existing_config_files_receive_defaults_for_new_auth_fields() {
        let config: OPanelConfig = serde_json::from_value(json!({
            "host": "127.0.0.1",
            "port": 8080,
        }))
        .unwrap();

        assert_eq!(config.host, "127.0.0.1");
        assert_eq!(config.port, 8080);
        assert!(config.access_key.is_empty());
        assert!(config.salt.is_empty());
        assert!(!config.cookie_secure);
        assert!(!config.proxy_headers);
    }

    #[test]
    fn secure_random_strings_use_the_expected_alphabet() {
        let value = secure_random_string(4_096).unwrap();
        assert_eq!(value.len(), 4_096);
        assert!(
            value
                .bytes()
                .all(|character| RANDOM_CHARACTERS.contains(&character))
        );
    }

    #[tokio::test]
    async fn first_start_generates_and_persists_credentials() {
        let (directory, storage) = test_storage().await;
        tokio::fs::write(directory.child("INITIAL_ACCESS_KEY.txt"), "stale")
            .await
            .unwrap();
        let manager = ConfigManager::new(manager_context());

        manager.initialize(&storage).await.unwrap();

        let config = manager.get();
        assert_eq!(config.access_key.len(), 32);
        assert_eq!(config.salt.len(), 6);
        assert!(
            config
                .salt
                .bytes()
                .all(|character| RANDOM_CHARACTERS.contains(&character))
        );
        let plaintext = storage.read_text(&INITIAL_ACCESS_KEY_FILE).await.unwrap();
        let access_key = plaintext
            .strip_prefix(INITIAL_ACCESS_KEY_TEMPLATE)
            .expect("the plaintext file should contain the bilingual warning");
        assert_eq!(access_key.len(), 12);
        assert_eq!(config.access_key, md5_hex(&md5_hex(access_key)));
        assert_eq!(
            storage.load_json(&CONFIG_FILE).await.unwrap().value,
            *config
        );
        assert!(manager.take_initial_access_key_notice());
        assert!(!manager.take_initial_access_key_notice());

        let stored: serde_json::Value = serde_json::from_slice(
            &tokio::fs::read(directory.child("config.json"))
                .await
                .unwrap(),
        )
        .unwrap();
        assert!(stored.get("accessKey").is_some());
        assert!(stored.get("access_key").is_none());
    }

    #[tokio::test]
    async fn later_start_keeps_credentials_and_deletes_plaintext_key() {
        let (directory, storage) = test_storage().await;
        let first_manager = ConfigManager::new(manager_context());
        first_manager.initialize(&storage).await.unwrap();
        let first_config = first_manager.get();
        assert!(directory.child("INITIAL_ACCESS_KEY.txt").is_file());

        let config_path = directory.child("config.json");
        let stored_with_future_field = format!(
            concat!(
                "{{\"host\":\"{}\",\"port\":{},\"accessKey\":\"{}\",",
                "\"salt\":\"{}\",\"cookieSecure\":{},\"proxyHeaders\":{},",
                "\"futureOption\":{{\"enabled\":true}}}}\n"
            ),
            first_config.host,
            first_config.port,
            first_config.access_key,
            first_config.salt,
            first_config.cookie_secure,
            first_config.proxy_headers,
        );
        tokio::fs::write(&config_path, stored_with_future_field.as_bytes())
            .await
            .unwrap();
        let bytes_before_restart = tokio::fs::read(&config_path).await.unwrap();

        let second_manager = ConfigManager::new(manager_context());
        second_manager.initialize(&storage).await.unwrap();

        assert_eq!(*second_manager.get(), *first_config);
        assert_eq!(
            tokio::fs::read(&config_path).await.unwrap(),
            bytes_before_restart,
            "a complete configuration must not be rewritten during startup"
        );
        assert!(!directory.child("INITIAL_ACCESS_KEY.txt").exists());
        assert!(!second_manager.take_initial_access_key_notice());
    }

    #[tokio::test]
    async fn fills_only_a_whitespace_salt_without_exposing_an_access_key() {
        let (directory, storage) = test_storage().await;
        let config_path = directory.child("config.json");
        tokio::fs::write(
            &config_path,
            br#"{
                "host": "0.0.0.0",
                "port": 3000,
                "accessKey": "stored-access-key",
                "salt": " \t\n",
                "cookieSecure": false,
                "proxyHeaders": false,
                "futureOption": 42
            }"#,
        )
        .await
        .unwrap();
        let manager = ConfigManager::new(manager_context());

        manager.initialize(&storage).await.unwrap();

        let config = manager.get();
        assert_eq!(config.access_key, "stored-access-key");
        assert_eq!(config.salt.len(), 6);
        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(config_path).await.unwrap()).unwrap();
        assert_eq!(stored["futureOption"], 42);
        assert_eq!(stored["salt"], config.salt);
        assert!(!directory.child("INITIAL_ACCESS_KEY.txt").exists());
        assert!(!manager.take_initial_access_key_notice());
    }

    #[tokio::test]
    async fn regenerates_a_whitespace_access_key() {
        let (directory, storage) = test_storage().await;
        let config_path = directory.child("config.json");
        tokio::fs::write(
            &config_path,
            br#"{
                "host": "0.0.0.0",
                "port": 3000,
                "accessKey": " \t\n",
                "salt": "stored-salt",
                "cookieSecure": false,
                "proxyHeaders": false,
                "futureOption": 42
            }"#,
        )
        .await
        .unwrap();
        let manager = ConfigManager::new(manager_context());

        manager.initialize(&storage).await.unwrap();

        let config = manager.get();
        let plaintext = storage.read_text(&INITIAL_ACCESS_KEY_FILE).await.unwrap();
        let access_key = plaintext
            .strip_prefix(INITIAL_ACCESS_KEY_TEMPLATE)
            .expect("the plaintext file should contain the bilingual warning");
        assert_eq!(access_key.len(), 12);
        assert_eq!(config.access_key, md5_hex(&md5_hex(access_key)));
        assert_eq!(config.salt, "stored-salt");

        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(config_path).await.unwrap()).unwrap();
        assert_eq!(stored["accessKey"], config.access_key);
        assert_eq!(stored["futureOption"], 42);
        assert!(manager.take_initial_access_key_notice());
    }

    #[tokio::test]
    async fn generated_credentials_preserve_unknown_configuration_fields() {
        let (directory, storage) = test_storage().await;
        let config_path = directory.child("config.json");
        tokio::fs::write(
            &config_path,
            br#"{
                "host": "127.0.0.1",
                "port": 8080,
                "futureOption": { "enabled": true }
            }"#,
        )
        .await
        .unwrap();
        let manager = ConfigManager::new(manager_context());

        manager.initialize(&storage).await.unwrap();

        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(config_path).await.unwrap()).unwrap();
        assert_eq!(stored["futureOption"], json!({ "enabled": true }));
        assert_eq!(stored["accessKey"], manager.get().access_key);
        assert_eq!(stored["salt"], manager.get().salt);
        assert!(manager.take_initial_access_key_notice());
    }

    #[tokio::test]
    async fn replacing_configuration_preserves_unknown_fields() {
        let (directory, storage) = test_storage().await;
        let config_path = directory.child("config.json");
        tokio::fs::write(
            &config_path,
            br#"{
                "host": "0.0.0.0",
                "port": 3000,
                "accessKey": "stored-access-key",
                "salt": "stored-salt",
                "cookieSecure": false,
                "proxyHeaders": false,
                "futureOption": { "enabled": true }
            }"#,
        )
        .await
        .unwrap();
        let manager = ConfigManager::new(manager_context());
        manager.initialize(&storage).await.unwrap();
        let replacement = OPanelConfig {
            port: 4000,
            ..(*manager.get()).clone()
        };

        manager
            .replace_in_storage(&storage, replacement.clone())
            .await
            .unwrap();

        assert_eq!(*manager.get(), replacement);
        let stored: serde_json::Value =
            serde_json::from_slice(&tokio::fs::read(config_path).await.unwrap()).unwrap();
        assert_eq!(stored["port"], 4000);
        assert_eq!(stored["futureOption"], json!({ "enabled": true }));
    }

    #[tokio::test]
    async fn invalid_json_aborts_initialization_without_overwriting_the_file() {
        let (directory, storage) = test_storage().await;
        let config_path = directory.child("config.json");
        let invalid = b"{\"accessKey\":";
        tokio::fs::write(&config_path, invalid).await.unwrap();
        let manager = ConfigManager::new(manager_context());

        assert!(matches!(
            manager.initialize(&storage).await,
            Err(super::ConfigManagerError::Storage(_))
        ));
        assert_eq!(tokio::fs::read(config_path).await.unwrap(), invalid);
        assert_eq!(*manager.get(), OPanelConfig::default());
        assert!(!directory.child("INITIAL_ACCESS_KEY.txt").exists());
        assert!(!manager.take_initial_access_key_notice());
    }

    #[tokio::test]
    async fn stale_plaintext_deletion_failure_aborts_initialization() {
        let (directory, storage) = test_storage().await;
        tokio::fs::create_dir(directory.child("INITIAL_ACCESS_KEY.txt"))
            .await
            .unwrap();
        let manager = ConfigManager::new(manager_context());

        assert!(matches!(
            manager.initialize(&storage).await,
            Err(super::ConfigManagerError::Storage(
                StorageError::Delete { .. }
            ))
        ));
        assert_eq!(*manager.get(), OPanelConfig::default());
        assert!(!manager.take_initial_access_key_notice());
        assert!(!directory.child("config.json").exists());
    }

    #[tokio::test]
    async fn failed_persistent_replace_does_not_publish_the_new_snapshot() {
        let (directory, storage) = test_storage().await;
        let manager = ConfigManager::new(manager_context());
        manager.initialize(&storage).await.unwrap();
        let original = manager.get();

        tokio::fs::remove_dir_all(directory.path()).await.unwrap();
        tokio::fs::write(directory.path(), "not a directory")
            .await
            .unwrap();
        let replacement = OPanelConfig {
            host: "127.0.0.1".to_string(),
            port: 1,
            ..(*original).clone()
        };

        assert!(matches!(
            manager.replace_in_storage(&storage, replacement).await,
            Err(super::ConfigManagerError::Storage(_))
        ));
        assert_eq!(*manager.get(), *original);
    }

    #[tokio::test]
    async fn concurrent_replacements_keep_disk_and_memory_in_sync() {
        let (_directory, storage) = test_storage().await;
        let storage = Arc::new(storage);
        let manager = Arc::new(ConfigManager::new(manager_context()));
        manager.initialize(storage.as_ref()).await.unwrap();
        let original = manager.get();
        let mut replacements = Vec::new();

        for port in 4_000..4_050 {
            let storage = Arc::clone(&storage);
            let manager = Arc::clone(&manager);
            let config = OPanelConfig {
                port,
                ..(*original).clone()
            };
            replacements.push(tokio::spawn(async move {
                manager
                    .replace_in_storage(storage.as_ref(), config)
                    .await
                    .unwrap();
            }));
        }

        for replacement in replacements {
            replacement.await.unwrap();
        }

        let stored = storage.load_json(&CONFIG_FILE).await.unwrap().value;
        assert_eq!(*manager.get(), stored);
    }
}
