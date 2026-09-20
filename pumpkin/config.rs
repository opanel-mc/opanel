use std::sync::Arc;

use arc_swap::ArcSwap;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OPanelConfig {
    pub host: String,
    pub port: u16,
}

impl Default for OPanelConfig {
    fn default() -> Self {
        Self {
            host: "0.0.0.0".to_string(),
            port: 3000,
        }
    }
}

pub struct ConfigManager {
    config: ArcSwap<OPanelConfig>,
}

impl ConfigManager {
    pub fn new(config: OPanelConfig) -> Self {
        Self {
            config: ArcSwap::from_pointee(config),
        }
    }

    pub fn get(&self) -> Arc<OPanelConfig> {
        self.config.load_full()
    }

    #[allow(dead_code)]
    pub fn replace(&self, config: OPanelConfig) {
        self.config.store(Arc::new(config));
    }
}

#[cfg(test)]
mod tests {
    use super::{ConfigManager, OPanelConfig};

    #[test]
    fn default_config_matches_the_web_server_defaults() {
        let config = OPanelConfig::default();

        assert_eq!(config.host, "0.0.0.0");
        assert_eq!(config.port, 3000);
    }

    #[test]
    fn config_manager_replaces_snapshots() {
        let manager = ConfigManager::new(OPanelConfig::default());
        manager.replace(OPanelConfig {
            host: "127.0.0.1".to_string(),
            port: 0,
        });

        let config = manager.get();
        assert_eq!(config.host, "127.0.0.1");
        assert_eq!(config.port, 0);
    }
}
