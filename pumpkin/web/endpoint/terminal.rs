use std::sync::Arc;

use axum::{
    extract::WebSocketUpgrade,
    routing::{MethodRouter, any},
};
use axum_extra::extract::CookieJar;
use serde::Deserialize;
use serde_json::Value;
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
};
use tokio_util::sync::CancellationToken;

use crate::{
    managers::Manager,
    terminal::{ConsoleLog, LogListenerManager},
    utils::server,
};

use super::{
    Endpoint, EndpointError, Packet, SessionAuthenticator, WsSession, handle_endpoint_result,
    upgrade,
};

pub(super) fn route(
    logs: Arc<LogListenerManager>,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter {
    any(move |ws: WebSocketUpgrade, cookies: CookieJar| {
        let endpoint = Arc::new(TerminalEndpoint::new(Arc::clone(&logs)));
        let authenticate = Arc::clone(&authenticate);
        let shutdown = shutdown.clone();
        async move { upgrade(ws, endpoint, authenticate, cookies, shutdown) }
    })
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AutocompleteData {
    command: String,
    arg_index: usize,
}

struct TerminalEndpoint {
    logs: Arc<LogListenerManager>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl TerminalEndpoint {
    fn new(logs: Arc<LogListenerManager>) -> Self {
        Self {
            logs,
            worker: Mutex::new(None),
        }
    }
}

impl Endpoint for TerminalEndpoint {
    async fn on_connect(&self, session: &WsSession) -> Result<(), EndpointError> {
        let (history, updates) = self.logs.subscribe();
        session.send(Packet::new("init", history))?;
        *self.worker.lock().await = Some(tokio::spawn(forward_logs(session.clone(), updates)));
        Ok(())
    }

    async fn on_packet(
        &self,
        session: &WsSession,
        packet: Packet<Value>,
    ) -> Result<(), EndpointError> {
        match packet.kind.as_str() {
            "command" => {
                let command: String = serde_json::from_value(packet.data)
                    .map_err(|_| EndpointError::InvalidPacket)?;
                let opanel = self.logs.opanel().map_err(|_| {
                    EndpointError::ServiceUnavailable("OPanel is no longer available.")
                })?;
                let command = command.strip_prefix('/').unwrap_or(&command).to_string();
                let output =
                    server::send_command_with_output(&opanel.context().server, command).await;
                self.logs.track_command_output(output);
            }
            "autocomplete" => {
                let data: AutocompleteData = serde_json::from_value(packet.data)
                    .map_err(|_| EndpointError::InvalidPacket)?;
                if data.arg_index == 0 {
                    return Err(EndpointError::InvalidPacket);
                }
                let opanel = self.logs.opanel().map_err(|_| {
                    EndpointError::ServiceUnavailable("OPanel is no longer available.")
                })?;
                let suggestions = server::get_command_suggestions(
                    &opanel.context().server,
                    &data.command,
                    data.arg_index,
                );
                session.send(Packet::new("autocomplete", suggestions))?;
            }
            _ => {}
        }
        Ok(())
    }

    async fn on_disconnect(&self, _session: &WsSession) {
        if let Some(worker) = self.worker.lock().await.take() {
            worker.abort();
            let _ = worker.await;
        }
    }
}

async fn forward_logs(session: WsSession, mut updates: broadcast::Receiver<ConsoleLog>) {
    loop {
        let result = match updates.recv().await {
            Ok(log) => session.send(Packet::new("log", log)),
            Err(broadcast::error::RecvError::Lagged(_)) => Err(EndpointError::SlowConsumer),
            Err(broadcast::error::RecvError::Closed) => break,
        };
        if handle_endpoint_result(result, &session) {
            break;
        }
    }
}

#[cfg(test)]
mod tests {
    use std::{
        sync::{Weak, atomic::AtomicBool},
        time::Duration,
    };

    use axum::Router;
    use futures_util::{SinkExt, StreamExt};
    use serde_json::json;
    use tokio::{net::TcpListener, sync::mpsc, time::timeout};
    use tokio_tungstenite::{connect_async, tungstenite::Message};

    use crate::managers::ManagerContext;

    use super::*;

    fn log_manager() -> Arc<LogListenerManager> {
        Arc::new(LogListenerManager::new(ManagerContext::new(
            Weak::new(),
            CancellationToken::new(),
        )))
    }

    async fn receive(
        socket: &mut tokio_tungstenite::WebSocketStream<
            tokio_tungstenite::MaybeTlsStream<tokio::net::TcpStream>,
        >,
    ) -> Value {
        let message = timeout(Duration::from_secs(2), socket.next())
            .await
            .unwrap()
            .unwrap()
            .unwrap();
        serde_json::from_str(message.to_text().unwrap()).unwrap()
    }

    #[tokio::test]
    async fn terminal_sends_history_and_broadcasts_logs_to_independent_connections() {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let logs = log_manager();
        logs.record_test_lines(vec!["history".into()]);
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let router = Router::new().route(
            "/terminal",
            route(Arc::clone(&logs), Arc::new(|_| true), shutdown.clone()),
        );
        let server = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(server_shutdown.cancelled_owned())
                .await
        });
        let (mut first, _) = connect_async(format!("ws://{address}/terminal"))
            .await
            .unwrap();
        let (mut second, _) = connect_async(format!("ws://{address}/terminal"))
            .await
            .unwrap();
        for socket in [&mut first, &mut second] {
            assert_eq!(
                receive(socket).await,
                json!({"type": "connect", "data": null})
            );
            let init = receive(socket).await;
            assert_eq!(init["type"], "init");
            assert_eq!(init["data"].as_array().unwrap().len(), 1);
            assert_eq!(init["data"][0]["line"], "history");
        }
        logs.record_test_lines(vec!["live".into()]);
        for socket in [&mut first, &mut second] {
            let log = receive(socket).await;
            assert_eq!(log["type"], "log");
            assert_eq!(log["data"]["line"], "live");
        }
        first.close(None).await.unwrap();
        logs.record_test_lines(vec!["still connected".into()]);
        assert_eq!(
            receive(&mut second).await["data"]["line"],
            "still connected"
        );

        second
            .send(Message::Text(
                json!({"type": "command", "data": 42}).to_string().into(),
            ))
            .await
            .unwrap();
        assert_eq!(
            receive(&mut second).await,
            json!({"type": "error", "data": 400})
        );
        let close = timeout(Duration::from_secs(2), second.next())
            .await
            .unwrap()
            .unwrap()
            .unwrap();
        assert!(matches!(close, Message::Close(Some(frame)) if u16::from(frame.code) == 1007));
        shutdown.cancel();
        timeout(Duration::from_secs(2), server)
            .await
            .unwrap()
            .unwrap()
            .unwrap();
    }

    #[tokio::test]
    async fn terminal_rejects_invalid_payloads_and_releases_its_worker_on_disconnect() {
        let (sender, mut receiver) = mpsc::channel(4);
        let (close_sender, _close_receiver) = mpsc::unbounded_channel();
        let session = WsSession {
            sender,
            close_sender,
            closing: Arc::new(AtomicBool::new(false)),
        };
        let logs = log_manager();
        let endpoint = TerminalEndpoint::new(Arc::clone(&logs));
        endpoint.on_connect(&session).await.unwrap();
        receiver.recv().await.unwrap();
        for packet in [
            Packet::new("command", Value::Null),
            Packet::new("command", json!({"command": "help"})),
            Packet::new("autocomplete", json!({"command": "time", "argIndex": -1})),
            Packet::new("autocomplete", json!({"command": "time", "argIndex": 0})),
            Packet::new("autocomplete", json!({"command": "time"})),
        ] {
            assert!(matches!(
                endpoint.on_packet(&session, packet).await,
                Err(EndpointError::InvalidPacket)
            ));
        }
        endpoint
            .on_packet(&session, Packet::new("unknown", Value::Null))
            .await
            .unwrap();
        endpoint.on_disconnect(&session).await;
        logs.record_test_lines(vec!["after disconnect".into()]);
        assert!(matches!(
            receiver.try_recv(),
            Err(mpsc::error::TryRecvError::Empty)
        ));
        assert_eq!(Arc::strong_count(&session.closing), 1);
    }

    #[tokio::test]
    async fn terminal_closes_slow_consumers() {
        for lagged in [false, true] {
            let (sender, _receiver) = mpsc::channel(1);
            let (close_sender, mut close_receiver) = mpsc::unbounded_channel();
            let session = WsSession {
                sender,
                close_sender,
                closing: Arc::new(AtomicBool::new(false)),
            };
            let logs = log_manager();
            let (_, subscription) = logs.subscribe();
            logs.record_test_lines(vec!["log".into(); if lagged { 1025 } else { 2 }]);
            timeout(Duration::from_secs(1), forward_logs(session, subscription))
                .await
                .unwrap();
            assert_eq!(close_receiver.recv().await.unwrap().frame.code, 1013);
        }
    }
}
