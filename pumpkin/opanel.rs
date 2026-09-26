use std::{path::PathBuf, sync::Arc};

use pumpkin::plugin::Context;
use thiserror::Error;
use tokio_util::sync::CancellationToken;

use crate::{
    config::OPanelConfig,
    managers::{ManagerContext, ManagerLifecycleError, Managers},
    storage::{Storage, StorageError},
};

pub struct OPanel {
    context: Arc<Context>,
    storage: Arc<Storage>,
    shutdown: CancellationToken,
    managers: Managers,
}

#[derive(Debug, Error)]
pub(crate) enum OPanelInitError {
    #[error("failed to initialize OPanel storage: {0}")]
    Storage(#[from] StorageError),
    #[error("failed to initialize OPanel managers: {0}")]
    Managers(#[from] ManagerLifecycleError),
}

impl OPanel {
    pub(crate) async fn initialize(context: Arc<Context>) -> Result<Arc<Self>, OPanelInitError> {
        let storage = Arc::new(Storage::open(PathBuf::from("opanel")).await?);
        let shutdown = CancellationToken::new();
        let opanel = Arc::new_cyclic(move |opanel| {
            let manager_context = ManagerContext::new(opanel.clone(), shutdown.clone());
            Self {
                context,
                storage,
                shutdown,
                managers: Managers::new(manager_context),
            }
        });

        opanel.managers.start().await?;
        Ok(opanel)
    }

    #[allow(dead_code)]
    pub fn context(&self) -> Arc<Context> {
        Arc::clone(&self.context)
    }

    pub fn config(&self) -> Arc<OPanelConfig> {
        self.managers().config().get()
    }

    #[allow(dead_code)]
    pub(crate) fn storage(&self) -> Arc<Storage> {
        Arc::clone(&self.storage)
    }

    pub(crate) fn managers(&self) -> &Managers {
        &self.managers
    }

    pub(crate) async fn shutdown(&self) -> Result<(), ManagerLifecycleError> {
        self.shutdown.cancel();
        self.managers.shutdown().await
    }
}

impl Drop for OPanel {
    fn drop(&mut self) {
        // Wake manager tasks even if the host drops the plugin without completing async shutdown.
        self.shutdown.cancel();
    }
}
