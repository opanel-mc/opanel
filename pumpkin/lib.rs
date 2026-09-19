use std::sync::Arc;

use pumpkin::plugin::Context;
use pumpkin_api_macros::{plugin_impl, plugin_method};

use crate::utils::log::info;

mod event;
mod map;
mod monitor;
mod task;
mod utils;
mod web;

#[plugin_method]
async fn on_load(&self, context: Arc<Context>) -> Result<(), String> {
    context.init_log();
    info("OPanel loaded");
    Ok(())
}

#[plugin_method]
async fn on_unload(&self, _context: Arc<Context>) -> Result<(), String> {
    info("OPanel unloaded");
    Ok(())
}

#[plugin_impl]
struct OPanel;

impl OPanel {
    fn new() -> Self {
        Self
    }
}
