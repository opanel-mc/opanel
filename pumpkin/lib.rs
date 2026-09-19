use pumpkin_plugin_api::{Context, Plugin, PluginMetadata, register_plugin};

use crate::utils::log::info;

mod event;
mod map;
mod monitor;
mod task;
mod utils;
mod web;

struct OPanel;

impl Plugin for OPanel {
    fn new() -> Self {
        OPanel
    }

    fn metadata(&self) -> PluginMetadata {
        PluginMetadata {
            name: "OPanel".into(),
            version: env!("CARGO_PKG_VERSION").into(),
            authors: vec![env!("CARGO_PKG_AUTHORS").into()],
            description: env!("CARGO_PKG_DESCRIPTION").into(),
            dependencies: vec![],
            permissions: vec![],
        }
    }

    fn on_load(&self, _context: Context) -> pumpkin_plugin_api::Result<()> {
        info("Hello World");
        Ok(())
    }

    fn on_unload(&self, _context: Context) -> pumpkin_plugin_api::Result<()> {
        info("OPanel unloaded");
        Ok(())
    }
}

register_plugin!(OPanel);
