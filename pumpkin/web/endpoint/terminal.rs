use std::sync::Arc;

use crate::opanel::OPanel;

use super::{Endpoint, EndpointError, WsSession};

pub(super) struct TerminalEndpoint {
    #[allow(dead_code)]
    opanel: Arc<OPanel>,
}

impl TerminalEndpoint {
    pub(super) fn new(opanel: Arc<OPanel>) -> Self {
        Self { opanel }
    }
}

impl Endpoint for TerminalEndpoint {
    async fn on_connect(&self, _session: &WsSession) -> Result<(), EndpointError> {
        Err(EndpointError::NotImplemented)
    }
}
