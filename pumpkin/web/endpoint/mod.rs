use std::{future::Future, sync::Arc, time::Duration};

use axum::{
    extract::{
        WebSocketUpgrade,
        ws::{CloseFrame, Message, WebSocket},
    },
    response::Response,
    routing::any,
};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;
use tokio::{sync::mpsc, task::JoinHandle, time::timeout};
use tokio_util::sync::CancellationToken;

use crate::opanel::OPanel;

mod hello;

const CONNECT: &str = "connect";
const PING: &str = "ping";
const PONG: &str = "pong";
const ERROR: &str = "error";
const MAX_OUTGOING_MESSAGES: usize = 1024;
const MAX_MESSAGE_SIZE: usize = 1024 * 1024;
const CLOSE_SEND_TIMEOUT: Duration = Duration::from_millis(100);

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
}

impl WsSession {
    pub fn send<T: Serialize>(&self, packet: Packet<T>) -> Result<(), EndpointError> {
        let message = serde_json::to_string(&packet)?;
        self.sender
            .try_send(Message::Text(message.into()))
            .map_err(|error| match error {
                mpsc::error::TrySendError::Full(_) => EndpointError::SlowConsumer,
                mpsc::error::TrySendError::Closed(_) => EndpointError::Closed,
            })
    }

    async fn close(&self, code: u16, reason: &'static str) {
        let close = Message::Close(Some(CloseFrame {
            code,
            reason: reason.into(),
        }));
        let _ = timeout(CLOSE_SEND_TIMEOUT, self.sender.send(close)).await;
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
    hello::router(opanel, shutdown)
        .route("/", any(super::response::not_found))
        .fallback(super::response::not_found)
}

pub fn upgrade<E>(ws: WebSocketUpgrade, endpoint: Arc<E>, shutdown: CancellationToken) -> Response
where
    E: Endpoint,
{
    ws.max_message_size(MAX_MESSAGE_SIZE)
        .on_upgrade(move |socket| serve(socket, endpoint, shutdown))
}

async fn serve<E>(socket: WebSocket, endpoint: Arc<E>, shutdown: CancellationToken)
where
    E: Endpoint,
{
    let (mut socket_sender, mut socket_receiver) = socket.split();
    let (sender, mut receiver) = mpsc::channel(MAX_OUTGOING_MESSAGES);
    let session = WsSession { sender };

    let writer: JoinHandle<()> = tokio::spawn(async move {
        while let Some(message) = receiver.recv().await {
            let is_close = matches!(message, Message::Close(_));
            if socket_sender.send(message).await.is_err() || is_close {
                break;
            }
        }
    });

    if session
        .send(Packet::new(CONNECT, Option::<()>::None))
        .is_err()
    {
        writer.abort();
        return;
    }

    if handle_endpoint_result(endpoint.on_connect(&session).await, &session).await {
        endpoint.on_disconnect(&session).await;
        drop(session);
        finish_writer(writer).await;
        return;
    }

    loop {
        tokio::select! {
            () = shutdown.cancelled() => {
                session.close(1001, "Server is stopping.").await;
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
                            let _ = session.send(Packet::new(ERROR, 400));
                            session.close(1007, "Invalid JSON packet.").await;
                            break;
                        };

                        if packet.kind == PING {
                            if session.send(Packet::new(PONG, Option::<()>::None)).is_err() {
                                session.close(1013, "Slow consumer.").await;
                                break;
                            }
                            continue;
                        }

                        if handle_endpoint_result(endpoint.on_packet(&session, packet).await, &session).await {
                            break;
                        }
                    }
                    Message::Binary(_) => {
                        let _ = session.send(Packet::new(ERROR, 400));
                        session.close(1003, "Binary messages are not supported.").await;
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

async fn handle_endpoint_result(result: Result<(), EndpointError>, session: &WsSession) -> bool {
    match result {
        Ok(()) => false,
        Err(EndpointError::SlowConsumer) => {
            session.close(1013, "Slow consumer.").await;
            true
        }
        Err(EndpointError::Closed) => true,
        Err(EndpointError::Serialize(_)) => {
            let _ = session.send(Packet::new(ERROR, 500));
            session
                .close(1011, "Failed to encode server message.")
                .await;
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
    use std::time::Duration;

    use futures_util::{SinkExt, StreamExt};
    use serde_json::Value;
    use tokio::{net::TcpListener, time::timeout};
    use tokio_tungstenite::{connect_async, tungstenite::Message};
    use tokio_util::sync::CancellationToken;

    use super::{Packet, hello};

    #[test]
    fn packet_uses_the_existing_wire_format() {
        let packet = Packet::new("hello", "Hello, world!");
        let value = serde_json::to_value(packet).expect("packet should serialize");

        assert_eq!(
            value,
            serde_json::json!({
                "type": "hello",
                "data": "Hello, world!"
            })
        );
    }

    #[tokio::test]
    async fn hello_endpoint_supports_packets_and_shutdown() {
        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("test listener should bind");
        let address = listener
            .local_addr()
            .expect("test listener should have an address");
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let server = tokio::spawn(async move {
            axum::serve(listener, hello::test_router(server_shutdown.clone()))
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut socket, _) = connect_async(format!("ws://{address}/hello"))
            .await
            .expect("websocket should connect");
        assert_packet(&mut socket, "connect", Value::Null).await;

        socket
            .send(Message::Text(
                serde_json::json!({"type": "hello", "data": null})
                    .to_string()
                    .into(),
            ))
            .await
            .expect("hello packet should send");
        assert_packet(
            &mut socket,
            "hello",
            Value::String("Hello, world!".to_string()),
        )
        .await;

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
            axum::serve(listener, hello::test_router(server_shutdown.clone()))
                .with_graceful_shutdown(async move {
                    server_shutdown.cancelled().await;
                })
                .await
        });

        let (mut invalid_json, _) = connect_async(format!("ws://{address}/hello"))
            .await
            .expect("websocket should connect");
        assert_packet(&mut invalid_json, "connect", Value::Null).await;
        invalid_json
            .send(Message::Text("{".into()))
            .await
            .expect("invalid JSON should send");
        assert_packet(&mut invalid_json, "error", Value::from(400)).await;
        assert_close_code(&mut invalid_json, 1007).await;

        let (mut binary, _) = connect_async(format!("ws://{address}/hello"))
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
