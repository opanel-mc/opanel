use std::sync::Arc;

use axum::Router;
use tokio_util::sync::CancellationToken;

use crate::opanel::OPanel;

use super::{Endpoint, EndpointError, WsSession, endpoint_route};

struct MonitorEndpoint {
    #[allow(dead_code)]
    opanel: Arc<OPanel>,
}

impl Endpoint for MonitorEndpoint {
    async fn on_connect(&self, _session: &WsSession) -> Result<(), EndpointError> {
        Err(EndpointError::NotImplemented)
    }
}

pub(super) fn router(opanel: Arc<OPanel>, shutdown: CancellationToken) -> Router {
    Router::new().route(
        "/monitor",
        endpoint_route(Arc::new(MonitorEndpoint { opanel }), shutdown),
    )
}
