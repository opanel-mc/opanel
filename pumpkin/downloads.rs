use std::{collections::HashMap, path::PathBuf};

use tokio::sync::Mutex;

const DOWNLOAD_ID_BYTES: usize = 8;

#[derive(Debug)]
pub(crate) struct DownloadEntry {
    pub(crate) path: PathBuf,
    pub(crate) delete_after_download: bool,
}

#[derive(Debug, Default)]
pub(crate) struct DownloadRegistry {
    entries: Mutex<HashMap<String, DownloadEntry>>,
}

impl DownloadRegistry {
    pub(crate) async fn register_path(
        &self,
        path: PathBuf,
        delete_after_download: bool,
    ) -> Result<String, getrandom::Error> {
        let mut entries = self.entries.lock().await;
        loop {
            let mut random = [0_u8; DOWNLOAD_ID_BYTES];
            getrandom::fill(&mut random)?;
            let id: String = random.iter().map(|byte| format!("{byte:02x}")).collect();
            if !entries.contains_key(&id) {
                entries.insert(
                    id.clone(),
                    DownloadEntry {
                        path: path.clone(),
                        delete_after_download,
                    },
                );
                return Ok(id);
            }
        }
    }

    pub(crate) async fn take(&self, id: &str) -> Option<DownloadEntry> {
        self.entries.lock().await.remove(id)
    }
}

impl Drop for DownloadRegistry {
    fn drop(&mut self) {
        for entry in self.entries.get_mut().values() {
            if entry.delete_after_download {
                let _ = std::fs::remove_file(&entry.path);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use super::DownloadRegistry;

    #[tokio::test]
    async fn registered_downloads_are_one_time_and_have_fixed_length_ids() {
        let registry = DownloadRegistry::default();
        let id = registry
            .register_path(PathBuf::from("archive.zip"), true)
            .await
            .unwrap();

        assert_eq!(id.len(), 16);
        let entry = registry.take(&id).await.unwrap();
        assert_eq!(entry.path, PathBuf::from("archive.zip"));
        assert!(entry.delete_after_download);
        assert!(registry.take(&id).await.is_none());
    }
}
