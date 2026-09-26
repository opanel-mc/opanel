use std::sync::Arc;

use crate::opanel::OPanel;

use super::{Endpoint, EndpointError, WsSession};

pub(super) struct InventoryEndpoint {
    #[allow(dead_code)]
    opanel: Arc<OPanel>,
}

impl InventoryEndpoint {
    pub(super) fn new(opanel: Arc<OPanel>) -> Self {
        Self { opanel }
    }
}

impl Endpoint for InventoryEndpoint {
    async fn on_connect(&self, _session: &WsSession) -> Result<(), EndpointError> {
        Err(EndpointError::NotImplemented)
    }
}
