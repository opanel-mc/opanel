use std::{
    future::Future,
    sync::{
        Arc,
        atomic::{AtomicBool, Ordering},
    },
    time::Duration,
};

use axum::{
    extract::{
        WebSocketUpgrade,
        ws::{CloseFrame, Message, WebSocket},
    },
    response::Response,
    routing::{MethodRouter, any},
};
use axum_extra::extract::CookieJar;
use futures_util::{Sink, SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;
use tokio::{sync::mpsc, task::JoinHandle, time::timeout};
use tokio_util::sync::CancellationToken;

use crate::opanel::OPanel;

mod inventory;
mod map;
mod monitor;
mod players;
mod terminal;

const CONNECT: &str = "connect";
const PING: &str = "ping";
const PONG: &str = "pong";
const ERROR: &str = "error";
const MAX_OUTGOING_MESSAGES: usize = 1024;
const MAX_MESSAGE_SIZE: usize = 1024 * 1024;

type SessionAuthenticator = Arc<dyn Fn(&CookieJar) -> bool + Send + Sync>;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Packet<T> {
    #[serde(rename = "type")]
    pub kind: String,
    pub data: T,
}

impl<T> Packet<T> {
    pub fn new(kind: impl Into<String>, data: T) -> Self {
        Self {
            kind: kind.into(),
            data,
        }
    }
}

#[derive(Debug, Error)]
pub enum EndpointError {
    #[error("websocket endpoint is not implemented")]
    NotImplemented,
    #[error("websocket connection is closed")]
    Closed,
    #[error("websocket client is not consuming messages fast enough")]
    SlowConsumer,
    #[error("failed to serialize websocket packet: {0}")]
    Serialize(#[from] serde_json::Error),
}

#[derive(Clone)]
pub struct WsSession {
    sender: mpsc::Sender<Message>,
    close_sender: mpsc::UnboundedSender<CloseRequest>,
    closing: Arc<AtomicBool>,
}

struct CloseRequest {
    final_message: Option<Message>,
    frame: CloseFrame,
}

impl WsSession {
    pub fn send<T: Serialize>(&self, packet: Packet<T>) -> Result<(), EndpointError> {
        if self.closing.load(Ordering::Acquire) {
            return Err(EndpointError::Closed);
        }

        let message = serde_json::to_string(&packet)?;
        self.sender
            .try_send(Message::Text(message.into()))
            .map_err(|error| match error {
                mpsc::error::TrySendError::Full(_) => EndpointError::SlowConsumer,
                mpsc::error::TrySendError::Closed(_) => EndpointError::Closed,
            })
    }

    fn close(&self, code: u16, reason: &'static str) {
        self.request_close(None, code, reason);
    }

    fn close_with_error(&self, error_code: u16, code: u16, reason: &'static str) {
        let final_message = serde_json::to_string(&Packet::new(ERROR, error_code))
            .ok()
            .map(|message| Message::Text(message.into()));
        self.request_close(final_message, code, reason);
    }

    fn request_close(&self, final_message: Option<Message>, code: u16, reason: &'static str) {
        if self.closing.swap(true, Ordering::AcqRel) {
            return;
        }

        let _ = self.close_sender.send(CloseRequest {
            final_message,
            frame: CloseFrame {
                code,
                reason: reason.into(),
            },
        });
    }
}

pub trait Endpoint: Send + Sync + 'static {
    fn on_connect(
        &self,
        _session: &WsSession,
    ) -> impl Future<Output = Result<(), EndpointError>> + Send {
        async { Ok(()) }
    }

    fn on_packet(
        &self,
        _session: &WsSession,
        _packet: Packet<Value>,
    ) -> impl Future<Output = Result<(), EndpointError>> + Send {
        async { Ok(()) }
    }

    fn on_disconnect(&self, _session: &WsSession) -> impl Future<Output = ()> + Send {
        async {}
    }
}

pub fn router(opanel: Arc<OPanel>, shutdown: CancellationToken) -> axum::Router {
    let authenticate = session_authenticator(Arc::clone(&opanel));

    axum::Router::new()
        .route(
            "/players",
            endpoint_route(
                Arc::new(players::PlayersEndpoint::new(Arc::clone(&opanel))),
                Arc::clone(&authenticate),
                shutdown.clone(),
            ),
        )
        .route(
            "/inventory/{uuid}",
            endpoint_route(
                Arc::new(inventory::InventoryEndpoint::new(Arc::clone(&opanel))),
                Arc::clone(&authenticate),
                shutdown.clone(),
            ),
        )
        .route(
            "/terminal",
            endpoint_route(
                Arc::new(terminal::TerminalEndpoint::new(Arc::clone(&opanel))),
                Arc::clone(&authenticate),
                shutdown.clone(),
            ),
        )
        .route(
            "/map",
            endpoint_route(
                Arc::new(map::MapEndpoint::new(Arc::clone(&opanel))),
                Arc::clone(&authenticate),
                shutdown.clone(),
            ),
        )
        .route(
            "/monitor",
            endpoint_route(
                Arc::new(monitor::MonitorEndpoint::new(opanel)),
                authenticate,
                shutdown,
            ),
        )
        .route("/", any(super::response::not_found))
        .fallback(super::response::not_found)
}

fn session_authenticator(opanel: Arc<OPanel>) -> SessionAuthenticator {
    Arc::new(move |cookies| {
        let Some(token) = cookies.get("token") else {
            return false;
        };
        let config = opanel.config();
        opanel
            .managers()
            .auth()
            .verify_token(token.value(), &config.access_key, &config.salt)
    })
}

pub(super) fn endpoint_route<E>(
    endpoint: Arc<E>,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter
where
    E: Endpoint,
{
    any(move |ws: WebSocketUpgrade, cookies: CookieJar| {
        let endpoint = Arc::clone(&endpoint);
        let authenticate = Arc::clone(&authenticate);
        let shutdown = shutdown.clone();
        async move { upgrade(ws, endpoint, authenticate, cookies, shutdown) }
    })
}

pub fn upgrade<E>(
    ws: WebSocketUpgrade,
    endpoint: Arc<E>,
    authenticate: SessionAuthenticator,
    cookies: CookieJar,
    shutdown: CancellationToken,
) -> Response
where
    E: Endpoint,
{
    ws.max_message_size(MAX_MESSAGE_SIZE)
        .on_upgrade(move |socket| serve(socket, endpoint, authenticate, cookies, shutdown))
}

async fn serve<E>(
    socket: WebSocket,
    endpoint: Arc<E>,
    authenticate: SessionAuthenticator,
    cookies: CookieJar,
    shutdown: CancellationToken,
) where
    E: Endpoint,
{
    let (mut socket_sender, mut socket_receiver) = socket.split();
    if !authenticate(&cookies) {
        let _ = socket_sender
            .send(Message::Close(Some(CloseFrame {
                code: 1008,
                reason: "Unauthorized.".into(),
            })))
            .await;
        return;
    }

    let (sender, receiver) = mpsc::channel(MAX_OUTGOING_MESSAGES);
    let (close_sender, close_receiver) = mpsc::unbounded_channel();
    let session = WsSession {
        sender,
        close_sender,
        closing: Arc::new(AtomicBool::new(false)),
    };

    let Ok(connect) = serde_json::to_string(&Packet::new(CONNECT, Option::<()>::None)) else {
        return;
    };
    if socket_sender
        .send(Message::Text(connect.into()))
        .await
        .is_err()
    {
        return;
    }

    let writer: JoinHandle<()> =
        tokio::spawn(write_messages(socket_sender, receiver, close_receiver));

    if handle_endpoint_result(endpoint.on_connect(&session).await, &session) {
        endpoint.on_disconnect(&session).await;
        drop(session);
        finish_writer(writer).await;
        return;
    }

    loop {
        tokio::select! {
            () = shutdown.cancelled() => {
                session.close(1001, "Server is stopping.");
                break;
            }
            message = socket_receiver.next() => {
                let Some(message) = message else {
                    break;
                };
                let Ok(message) = message else {
                    break;
                };

                match message {
                    Message::Text(text) => {
                        let packet = serde_json::from_str::<Packet<Value>>(&text);
                        let Ok(packet) = packet else {
                            session.close_with_error(400, 1007, "Invalid JSON packet.");
                            break;
                        };

                        if packet.kind == PING {
                            if session.send(Packet::new(PONG, Option::<()>::None)).is_err() {
                                session.close(1013, "Slow consumer.");
                                break;
                            }
                            continue;
                        }

                        if handle_endpoint_result(endpoint.on_packet(&session, packet).await, &session) {
                            break;
                        }
                    }
                    Message::Binary(_) => {
                        session.close_with_error(400, 1003, "Binary messages are not supported.");
                        break;
                    }
                    Message::Close(_) => break,
                    Message::Ping(_) | Message::Pong(_) => {}
                }
            }
        }
    }

    endpoint.on_disconnect(&session).await;
    drop(session);
    finish_writer(writer).await;
}

async fn write_messages<S>(
    mut socket_sender: S,
    mut receiver: mpsc::Receiver<Message>,
    mut close_receiver: mpsc::UnboundedReceiver<CloseRequest>,
) where
    S: Sink<Message> + Unpin,
{
    loop {
        let close = tokio::select! {
            biased;
            close = close_receiver.recv() => close,
            message = receiver.recv() => {
                let Some(message) = message else {
                    break;
                };

                // A close request must also interrupt a write already waiting on socket backpressure.
                tokio::select! {
                    biased;
                    close = close_receiver.recv() => close,
                    result = socket_sender.send(message) => {
                        if result.is_err() {
                            break;
                        }
                        continue;
                    }
                }
            }
        };

        let Some(close) = close else {
            break;
        };
        receiver.close();
        if let Some(message) = close.final_message
            && socket_sender.send(message).await.is_err()
        {
            break;
        }
        let _ = socket_sender.send(Message::Close(Some(close.frame))).await;
        break;
    }
}

fn handle_endpoint_result(result: Result<(), EndpointError>, session: &WsSession) -> bool {
    match result {
        Ok(()) => false,
        Err(EndpointError::NotImplemented) => {
            session.close_with_error(501, 1011, "Endpoint is not implemented.");
            true
        }
        Err(EndpointError::SlowConsumer) => {
            session.close(1013, "Slow consumer.");
            true
        }
        Err(EndpointError::Closed) => true,
        Err(EndpointError::Serialize(_)) => {
            session.close_with_error(500, 1011, "Failed to encode server message.");
            true
        }
    }
}

async fn finish_writer(mut writer: JoinHandle<()>) {
    if timeout(Duration::from_secs(1), &mut writer).await.is_err() {
        writer.abort();
        let _ = writer.await;
    }
}

#[cfg(test)]
mod tests {
    use std::{
        pin::Pin,
        sync::{Arc, Weak, atomic::AtomicBool},
        task::{Context, Poll},
        time::Duration,
    };

    use axum::{Router, extract::ws::Message as WsMessage};
    use futures_util::{Sink, SinkExt, StreamExt};
    use serde_json::Value;
    use tokio::{
        net::TcpListener,
        sync::{mpsc, oneshot},
        time::timeout,
    };
    use tokio_tungstenite::{
        connect_async,
        tungstenite::{
            Message,
            client::IntoClientRequest,
            http::{HeaderValue, header::COOKIE},
        },
    };
    use tokio_util::sync::CancellationToken;

    use crate::{managers::ManagerContext, web::AuthManager};

    use super::{
        CloseRequest, Endpoint, EndpointError, MAX_OUTGOING_MESSAGES, Packet, SessionAuthenticator,
        WsSession, endpoint_route, write_messages,
    };

    fn allow_all() -> SessionAuthenticator {
        Arc::new(|_| true)
    }

    fn test_router(shutdown: CancellationToken) -> Router {
        struct TestEndpoint;

        impl Endpoint for TestEndpoint {}

        Router::new().route(
            "/test",
            endpoint_route(Arc::new(TestEndpoint), allow_all(), shutdown),
        )
    }

    #[test]
    fn packet_uses_the_existing_wire_format() {
        let packet = Packet::new("example", "payload");
        let value = serde_json::to_value(packet).expect("packet should serialize");

        assert_eq!(
            value,
            serde_json::json!({
                "type": "example",
                "data": "payload"
            })
        );
    }

    #[tokio::test]
    async fn endpoint_authenticates_shared_jwt_after_upgrade_before_connect() {
        struct TestEndpoint;

        impl Endpoint for TestEndpoint {}

        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("test listener should bind");
        let address = listener
            .local_addr()
            .expect("test listener should have an address");
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let auth = Arc::new(AuthManager::new(ManagerContext::new(
            Weak::new(),
            CancellationToken::new(),
        )));
        let access_key = "0123456789abcdef0123456789abcdef";
        let salt = "abc123";
        let valid_token = auth.issue_token(access_key, salt);
        let authenticate: SessionAuthenticator = Arc::new(move |cookies| {
            cookies
                .get("token")
                .is_some_and(|token| auth.verify_token(token.value(), access_key, salt))
        });
        let router = Router::new().route(
            "/test",
            endpoint_route(Arc::new(TestEndpoint), authenticate, shutdown.clone()),
        );
        let server = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut unauthorized, _) = connect_async(format!("ws://{address}/test"))
            .await
            .expect("websocket upgrade should complete");
        assert_close_code(&mut unauthorized, 1008).await;

        let mut invalid_request = format!("ws://{address}/test")
            .into_client_request()
            .expect("websocket request should be valid");
        invalid_request
            .headers_mut()
            .insert(COOKIE, HeaderValue::from_static("token=invalid"));
        let (mut invalid, _) = connect_async(invalid_request)
            .await
            .expect("websocket upgrade should complete");
        assert_close_code(&mut invalid, 1008).await;

        let mut request = format!("ws://{address}/test")
            .into_client_request()
            .expect("websocket request should be valid");
        request.headers_mut().insert(
            COOKIE,
            HeaderValue::from_str(&format!("token={valid_token}"))
                .expect("signed token should be a valid cookie header"),
        );
        let (mut authorized, _) = connect_async(request)
            .await
            .expect("authenticated websocket should connect");
        assert_packet(&mut authorized, "connect", Value::Null).await;

        shutdown.cancel();
        assert_close_code(&mut authorized, 1001).await;
        timeout(Duration::from_secs(2), server)
            .await
            .expect("server should stop promptly")
            .expect("server task should join")
            .expect("server should stop cleanly");
    }

    #[tokio::test]
    async fn close_bypasses_a_full_outgoing_queue() {
        let (sender, _receiver) = tokio::sync::mpsc::channel(MAX_OUTGOING_MESSAGES);
        for _ in 0..MAX_OUTGOING_MESSAGES {
            sender
                .try_send(axum::extract::ws::Message::Text("queued".into()))
                .expect("application queue should have room");
        }

        let (close_sender, mut close_receiver) = tokio::sync::mpsc::unbounded_channel();
        let session = WsSession {
            sender,
            close_sender,
            closing: Arc::new(AtomicBool::new(false)),
        };

        assert!(matches!(
            session.send(Packet::new("overflow", Option::<()>::None)),
            Err(EndpointError::SlowConsumer)
        ));

        session.close(1013, "Slow consumer.");

        let close = timeout(Duration::from_millis(100), close_receiver.recv())
            .await
            .expect("close request should bypass the full application queue")
            .expect("close channel should remain open");
        assert!(close.final_message.is_none());
        assert_eq!(close.frame.code, 1013);
        assert!(matches!(
            session.send(Packet::new("after-close", Option::<()>::None)),
            Err(EndpointError::Closed)
        ));
    }

    struct BlockFirstWrite {
        started: Option<oneshot::Sender<()>>,
        writes: mpsc::UnboundedSender<WsMessage>,
    }

    impl Sink<WsMessage> for BlockFirstWrite {
        type Error = ();

        fn poll_ready(mut self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), ()>> {
            if let Some(started) = self.started.take() {
                let _ = started.send(());
                Poll::Pending
            } else {
                Poll::Ready(Ok(()))
            }
        }

        fn start_send(self: Pin<&mut Self>, message: WsMessage) -> Result<(), ()> {
            self.writes.send(message).map_err(|_| ())
        }

        fn poll_flush(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), ()>> {
            Poll::Ready(Ok(()))
        }

        fn poll_close(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), ()>> {
            Poll::Ready(Ok(()))
        }
    }

    #[tokio::test]
    async fn close_interrupts_a_blocked_write() {
        let (sender, receiver) = mpsc::channel(1);
        let (close_sender, close_receiver) = mpsc::unbounded_channel();
        let (started_sender, started_receiver) = oneshot::channel();
        let (writes, mut written) = mpsc::unbounded_channel();
        let sink = BlockFirstWrite {
            started: Some(started_sender),
            writes,
        };
        let writer = tokio::spawn(write_messages(sink, receiver, close_receiver));

        sender
            .try_send(WsMessage::Text("blocked application message".into()))
            .expect("application queue should have room");
        timeout(Duration::from_secs(1), started_receiver)
            .await
            .expect("application write should start")
            .expect("writer should report the blocked write");

        close_sender
            .send(CloseRequest {
                final_message: Some(WsMessage::Text("error".into())),
                frame: axum::extract::ws::CloseFrame {
                    code: 1013,
                    reason: "Slow consumer.".into(),
                },
            })
            .expect("close channel should remain open");

        timeout(Duration::from_secs(1), writer)
            .await
            .expect("close should interrupt the blocked write")
            .expect("writer should finish cleanly");
        assert!(matches!(written.recv().await, Some(WsMessage::Text(text)) if text == "error"));
        assert!(
            matches!(written.recv().await, Some(WsMessage::Close(Some(frame))) if frame.code == 1013)
        );
        assert!(
            written.recv().await.is_none(),
            "blocked application message should be discarded"
        );
    }

    #[tokio::test]
    async fn unimplemented_endpoint_reports_501_and_closes() {
        struct UnimplementedEndpoint;

        impl Endpoint for UnimplementedEndpoint {
            async fn on_connect(&self, _session: &WsSession) -> Result<(), EndpointError> {
                Err(EndpointError::NotImplemented)
            }
        }

        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("test listener should bind");
        let address = listener
            .local_addr()
            .expect("test listener should have an address");
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let router = Router::new().route(
            "/stub",
            endpoint_route(
                Arc::new(UnimplementedEndpoint),
                allow_all(),
                shutdown.clone(),
            ),
        );
        let server = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut socket, _) = connect_async(format!("ws://{address}/stub"))
            .await
            .expect("websocket should connect");
        assert_packet(&mut socket, "connect", Value::Null).await;
        assert_packet(&mut socket, "error", Value::from(501)).await;
        assert_close_code(&mut socket, 1011).await;

        shutdown.cancel();
        timeout(Duration::from_secs(2), server)
            .await
            .expect("server should stop promptly")
            .expect("server task should join")
            .expect("server should stop cleanly");
    }

    #[tokio::test]
    async fn endpoint_supports_ping_and_shutdown() {
        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("test listener should bind");
        let address = listener
            .local_addr()
            .expect("test listener should have an address");
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let server = tokio::spawn(async move {
            axum::serve(listener, test_router(server_shutdown.clone()))
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut socket, _) = connect_async(format!("ws://{address}/test"))
            .await
            .expect("websocket should connect");
        assert_packet(&mut socket, "connect", Value::Null).await;

        socket
            .send(Message::Text(
                serde_json::json!({"type": "ping", "data": null})
                    .to_string()
                    .into(),
            ))
            .await
            .expect("ping packet should send");
        assert_packet(&mut socket, "pong", Value::Null).await;

        shutdown.cancel();
        let close = timeout(Duration::from_secs(2), socket.next())
            .await
            .expect("server should close websocket promptly")
            .expect("websocket should yield a close message")
            .expect("close message should be valid");
        assert!(matches!(close, Message::Close(Some(frame)) if u16::from(frame.code) == 1001));

        timeout(Duration::from_secs(2), server)
            .await
            .expect("server should stop promptly")
            .expect("server task should join")
            .expect("server should stop cleanly");
    }

    #[tokio::test]
    async fn rejects_invalid_json_and_binary_messages() {
        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("test listener should bind");
        let address = listener
            .local_addr()
            .expect("test listener should have an address");
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let server = tokio::spawn(async move {
            axum::serve(listener, test_router(server_shutdown.clone()))
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut invalid_json, _) = connect_async(format!("ws://{address}/test"))
            .await
            .expect("websocket should connect");
        assert_packet(&mut invalid_json, "connect", Value::Null).await;
        invalid_json
            .send(Message::Text("{".into()))
            .await
            .expect("invalid JSON should send");
        assert_packet(&mut invalid_json, "error", Value::from(400)).await;
        assert_close_code(&mut invalid_json, 1007).await;

        let (mut binary, _) = connect_async(format!("ws://{address}/test"))
            .await
            .expect("second websocket should connect");
        assert_packet(&mut binary, "connect", Value::Null).await;
        binary
            .send(Message::Binary(Vec::new().into()))
            .await
            .expect("binary message should send");
        assert_packet(&mut binary, "error", Value::from(400)).await;
        assert_close_code(&mut binary, 1003).await;

        shutdown.cancel();
        timeout(Duration::from_secs(2), server)
            .await
            .expect("server should stop promptly")
            .expect("server task should join")
            .expect("server should stop cleanly");
    }

    async fn assert_packet(
        socket: &mut tokio_tungstenite::WebSocketStream<
            tokio_tungstenite::MaybeTlsStream<tokio::net::TcpStream>,
        >,
        kind: &str,
        data: Value,
    ) {
        let message = timeout(Duration::from_secs(2), socket.next())
            .await
            .expect("websocket should respond promptly")
            .expect("websocket should yield a message")
            .expect("websocket message should be valid");
        let Message::Text(text) = message else {
            panic!("expected text packet, got {message:?}");
        };
        let packet: Packet<Value> =
            serde_json::from_str(&text).expect("text message should contain a packet");
        assert_eq!(packet.kind, kind);
        assert_eq!(packet.data, data);
    }

    async fn assert_close_code(
        socket: &mut tokio_tungstenite::WebSocketStream<
            tokio_tungstenite::MaybeTlsStream<tokio::net::TcpStream>,
        >,
        expected_code: u16,
    ) {
        let message = timeout(Duration::from_secs(2), socket.next())
            .await
            .expect("websocket should close promptly")
            .expect("websocket should yield a close message")
            .expect("close message should be valid");
        assert!(
            matches!(message, Message::Close(Some(frame)) if u16::from(frame.code) == expected_code)
        );
    }
}
