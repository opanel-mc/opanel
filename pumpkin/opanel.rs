use std::sync::Arc;

use pumpkin::plugin::Context;

use crate::config::{ConfigManager, OPanelConfig};

pub struct OPanel {
    #[allow(dead_code)]
    context: Arc<Context>,
    config_manager: ConfigManager,
}

impl OPanel {
    pub fn new(context: Arc<Context>, config: OPanelConfig) -> Self {
        Self {
            context,
            config_manager: ConfigManager::new(config),
        }
    }

    #[allow(dead_code)]
    pub fn context(&self) -> Arc<Context> {
        Arc::clone(&self.context)
    }

    pub fn config(&self) -> Arc<OPanelConfig> {
        self.config_manager.get()
    }

    #[allow(dead_code)]
    pub fn config_manager(&self) -> &ConfigManager {
        &self.config_manager
    }
}
