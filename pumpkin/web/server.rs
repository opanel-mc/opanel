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
use tower_http::{
    cors::{AllowCredentials, AllowHeaders, AllowMethods, AllowOrigin, CorsLayer},
    trace::TraceLayer,
};
use tracing::info;

use crate::opanel::OPanel;

use super::{controller, endpoint, static_files};

const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(5);
const DEVELOPMENT_ORIGIN: &str = "http://localhost:3001";

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
    let controller_router = controller::router()
        .layer(cors_layer())
        .with_state(Arc::clone(&opanel));
    let endpoint_router = endpoint::router(opanel, shutdown);

    Router::new()
        .merge(controller_router)
        .nest("/socket", endpoint_router)
        .fallback(static_files::serve)
        .layer(middleware::from_fn(common_headers))
        .layer(TraceLayer::new_for_http())
}

fn cors_layer() -> CorsLayer {
    CorsLayer::new()
        .allow_origin(AllowOrigin::predicate(|origin, request| {
            is_open_api_path(request.uri.path())
                || (is_panel_path(request.uri.path()) && is_development_origin(origin))
        }))
        .allow_credentials(AllowCredentials::predicate(|origin, request| {
            is_panel_path(request.uri.path()) && is_development_origin(origin)
        }))
        .allow_methods(AllowMethods::mirror_request())
        .allow_headers(AllowHeaders::mirror_request())
}

fn is_open_api_path(path: &str) -> bool {
    is_path_or_descendant(path, "/open-api")
}

fn is_panel_path(path: &str) -> bool {
    ["/api", "/assets", "/file"]
        .into_iter()
        .any(|prefix| is_path_or_descendant(path, prefix))
}

fn is_path_or_descendant(path: &str, prefix: &str) -> bool {
    path == prefix
        || path
            .strip_prefix(prefix)
            .is_some_and(|suffix| suffix.starts_with('/'))
}

fn is_development_origin(origin: &HeaderValue) -> bool {
    origin.as_bytes() == DEVELOPMENT_ORIGIN.as_bytes()
}

async fn common_headers(request: Request, next: Next) -> Response {
    let mut response = next.run(request).await;
    response.headers_mut().insert(
        HeaderName::from_static("x-powered-by"),
        HeaderValue::from_static("OPanel"),
    );
    response
}

#[cfg(test)]
mod tests {
    use axum::{
        Router,
        body::Body,
        http::{
            HeaderValue, Method, Request, StatusCode,
            header::{
                ACCESS_CONTROL_ALLOW_CREDENTIALS, ACCESS_CONTROL_ALLOW_HEADERS,
                ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_ORIGIN,
                ACCESS_CONTROL_REQUEST_HEADERS, ACCESS_CONTROL_REQUEST_METHOD, ORIGIN,
            },
        },
    };
    use tower::ServiceExt;

    use super::{DEVELOPMENT_ORIGIN, cors_layer};

    fn test_router() -> Router {
        Router::new()
            .fallback(|| async { StatusCode::NOT_FOUND })
            .layer(cors_layer())
    }

    fn preflight_request(path: &str, origin: &str) -> Request<Body> {
        Request::builder()
            .method(Method::OPTIONS)
            .uri(path)
            .header(ORIGIN, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD, Method::PATCH.as_str())
            .header(ACCESS_CONTROL_REQUEST_HEADERS, "content-type, x-test")
            .body(Body::empty())
            .expect("preflight request should be valid")
    }

    #[tokio::test]
    async fn allows_development_origin_for_panel_routes_with_credentials() {
        for path in ["/api/test", "/assets/test", "/file/test"] {
            let response = test_router()
                .oneshot(preflight_request(path, DEVELOPMENT_ORIGIN))
                .await
                .expect("CORS preflight should succeed");

            assert_eq!(response.status(), StatusCode::OK);
            assert_eq!(
                response.headers().get(ACCESS_CONTROL_ALLOW_ORIGIN),
                Some(&HeaderValue::from_static(DEVELOPMENT_ORIGIN))
            );
            assert_eq!(
                response.headers().get(ACCESS_CONTROL_ALLOW_CREDENTIALS),
                Some(&HeaderValue::from_static("true"))
            );
            assert_eq!(
                response.headers().get(ACCESS_CONTROL_ALLOW_METHODS),
                Some(&HeaderValue::from_static("PATCH"))
            );
            assert_eq!(
                response.headers().get(ACCESS_CONTROL_ALLOW_HEADERS),
                Some(&HeaderValue::from_static("content-type, x-test"))
            );
        }
    }

    #[tokio::test]
    async fn rejects_other_origins_for_panel_routes() {
        let response = test_router()
            .oneshot(preflight_request("/api/test", "https://example.com"))
            .await
            .expect("CORS preflight should complete");

        assert_eq!(response.status(), StatusCode::OK);
        assert!(
            response
                .headers()
                .get(ACCESS_CONTROL_ALLOW_ORIGIN)
                .is_none()
        );
        assert!(
            response
                .headers()
                .get(ACCESS_CONTROL_ALLOW_CREDENTIALS)
                .is_none()
        );
    }

    #[tokio::test]
    async fn allows_any_origin_for_open_api_without_credentials() {
        let response = test_router()
            .oneshot(preflight_request("/open-api/test", "https://example.com"))
            .await
            .expect("CORS preflight should succeed");

        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(
            response.headers().get(ACCESS_CONTROL_ALLOW_ORIGIN),
            Some(&HeaderValue::from_static("https://example.com"))
        );
        assert!(
            response
                .headers()
                .get(ACCESS_CONTROL_ALLOW_CREDENTIALS)
                .is_none()
        );
    }
}
