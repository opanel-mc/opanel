use std::{io, net::SocketAddr, sync::Arc, time::Duration};

use axum::{
    Router,
    extract::Request,
    http::{HeaderValue, header::HeaderName},
    middleware::{self, Next},
    response::Response,
};
use thiserror::Error;
use tokio::{net::TcpListener, task::JoinHandle, time::timeout};
use tokio_util::sync::CancellationToken;
use tower_http::trace::TraceLayer;
use tracing::info;

use crate::opanel::OPanel;

use super::{controller, endpoint, static_files};

const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(5);

pub struct WebServer {
    #[allow(dead_code)]
    local_addr: SocketAddr,
    shutdown: CancellationToken,
    task: JoinHandle<Result<(), io::Error>>,
}

#[derive(Debug, Error)]
pub enum WebServerError {
    #[error("failed to bind web server to {address}: {source}")]
    Bind {
        address: String,
        #[source]
        source: io::Error,
    },
    #[error("web server task failed: {0}")]
    Join(#[from] tokio::task::JoinError),
    #[error("web server failed: {0}")]
    Serve(#[source] io::Error),
    #[error("web server did not stop within five seconds")]
    ShutdownTimeout,
}

impl WebServer {
    pub async fn start(opanel: Arc<OPanel>) -> Result<Self, WebServerError> {
        let config = opanel.config();
        let address = format!("{}:{}", config.host, config.port);
        let listener =
            TcpListener::bind(&address)
                .await
                .map_err(|source| WebServerError::Bind {
                    address: address.clone(),
                    source,
                })?;
        let local_addr = listener
            .local_addr()
            .map_err(|source| WebServerError::Bind {
                address: address.clone(),
                source,
            })?;

        let shutdown = CancellationToken::new();
        let router = build_router(opanel, shutdown.clone());
        let graceful_shutdown = shutdown.clone();
        let task = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(async move {
                    graceful_shutdown.cancelled().await;
                })
                .await
        });

        info!("OPanel web server is ready on {local_addr}");
        Ok(Self {
            local_addr,
            shutdown,
            task,
        })
    }

    #[allow(dead_code)]
    pub fn local_addr(&self) -> SocketAddr {
        self.local_addr
    }

    #[allow(dead_code)]
    pub fn is_running(&self) -> bool {
        !self.task.is_finished()
    }

    pub async fn shutdown(self) -> Result<(), WebServerError> {
        self.shutdown.cancel();
        let mut task = self.task;

        match timeout(SHUTDOWN_TIMEOUT, &mut task).await {
            Ok(result) => match result? {
                Ok(()) => {
                    info!("OPanel web server is stopped.");
                    Ok(())
                }
                Err(error) => Err(WebServerError::Serve(error)),
            },
            Err(_) => {
                task.abort();
                let _ = task.await;
                Err(WebServerError::ShutdownTimeout)
            }
        }
    }
}

fn build_router(opanel: Arc<OPanel>, shutdown: CancellationToken) -> Router {
    let controller_router = controller::router().with_state(Arc::clone(&opanel));
    let endpoint_router = endpoint::router(opanel, shutdown);

    Router::new()
        .merge(controller_router)
        .nest("/socket", endpoint_router)
        .fallback(static_files::serve)
        .layer(middleware::from_fn(common_headers))
        .layer(TraceLayer::new_for_http())
}

async fn common_headers(request: Request, next: Next) -> Response {
    let mut response = next.run(request).await;
    response.headers_mut().insert(
        HeaderName::from_static("x-powered-by"),
        HeaderValue::from_static("OPanel"),
    );
    response
}
