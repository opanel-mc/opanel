use std::sync::Arc;

use axum::{Router, extract::WebSocketUpgrade, response::Response, routing::any};
use serde_json::Value;
use tokio_util::sync::CancellationToken;

use crate::opanel::OPanel;

use super::{ERROR, Endpoint, EndpointError, Packet, WsSession, upgrade};

const HELLO: &str = "hello";

struct HelloEndpoint {
    #[allow(dead_code)]
    opanel: Arc<OPanel>,
}

impl Endpoint for HelloEndpoint {
    async fn on_packet(
        &self,
        session: &WsSession,
        packet: Packet<Value>,
    ) -> Result<(), EndpointError> {
        if packet.kind == HELLO {
            session.send(Packet::new(HELLO, "Hello, world!"))
        } else {
            session.send(Packet::new(ERROR, 400))
        }
    }
}

pub(super) fn router(opanel: Arc<OPanel>, shutdown: CancellationToken) -> Router {
    build_router(Arc::new(HelloEndpoint { opanel }), shutdown)
}

fn build_router<E>(endpoint: Arc<E>, shutdown: CancellationToken) -> Router
where
    E: Endpoint,
{
    Router::new().route(
        "/hello",
        any(move |ws: WebSocketUpgrade| {
            let endpoint = Arc::clone(&endpoint);
            let shutdown = shutdown.clone();
            async move { hello(ws, endpoint, shutdown) }
        }),
    )
}

fn hello<E>(ws: WebSocketUpgrade, endpoint: Arc<E>, shutdown: CancellationToken) -> Response
where
    E: Endpoint,
{
    upgrade(ws, endpoint, shutdown)
}

#[cfg(test)]
pub(super) fn test_router(shutdown: CancellationToken) -> Router {
    struct TestHelloEndpoint;

    impl Endpoint for TestHelloEndpoint {
        async fn on_packet(
            &self,
            session: &WsSession,
            packet: Packet<Value>,
        ) -> Result<(), EndpointError> {
            if packet.kind == HELLO {
                session.send(Packet::new(HELLO, "Hello, world!"))
            } else {
                session.send(Packet::new(ERROR, 400))
            }
        }
    }

    build_router(Arc::new(TestHelloEndpoint), shutdown)
}
