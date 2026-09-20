use std::sync::Arc;

use arc_swap::ArcSwap;

use crate::managers::{Manager, ManagerContext};

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
    context: ManagerContext,
    config: ArcSwap<OPanelConfig>,
}

impl ConfigManager {
    pub(crate) fn new(context: ManagerContext, config: OPanelConfig) -> Self {
        Self {
            context,
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

impl Manager for ConfigManager {
    fn name(&self) -> &'static str {
        "config"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Weak;

    use tokio_util::sync::CancellationToken;

    use super::{ConfigManager, OPanelConfig};
    use crate::managers::ManagerContext;

    fn manager_context() -> ManagerContext {
        ManagerContext::new(Weak::new(), CancellationToken::new())
    }

    #[test]
    fn default_config_matches_the_web_server_defaults() {
        let config = OPanelConfig::default();

        assert_eq!(config.host, "0.0.0.0");
        assert_eq!(config.port, 3000);
    }

    #[test]
    fn config_manager_replaces_snapshots() {
        let manager = ConfigManager::new(manager_context(), OPanelConfig::default());
        manager.replace(OPanelConfig {
            host: "127.0.0.1".to_string(),
            port: 0,
        });

        let config = manager.get();
        assert_eq!(config.host, "127.0.0.1");
        assert_eq!(config.port, 0);
    }
}
