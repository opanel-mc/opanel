use std::sync::Arc;

use crate::opanel::OPanel;

use super::{Endpoint, EndpointError, WsSession};

pub(super) struct PlayersEndpoint {
    #[allow(dead_code)]
    opanel: Arc<OPanel>,
}

impl PlayersEndpoint {
    pub(super) fn new(opanel: Arc<OPanel>) -> Self {
        Self { opanel }
    }
}

impl Endpoint for PlayersEndpoint {
    async fn on_connect(&self, _session: &WsSession) -> Result<(), EndpointError> {
        Err(EndpointError::NotImplemented)
    }
}
