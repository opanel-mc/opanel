use std::{collections::HashMap, sync::Arc};

use axum::{
    extract::{Query, WebSocketUpgrade},
    routing::{MethodRouter, any},
};
use axum_extra::extract::CookieJar;
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
};
use tokio_util::sync::CancellationToken;

use crate::monitor::{MAX_HISTORY_SIZE, MonitorData, MonitorManager};

use super::{
    Endpoint, EndpointError, Packet, SessionAuthenticator, WsSession, handle_endpoint_result,
    upgrade,
};

pub(super) fn route(
    monitor: Arc<MonitorManager>,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter {
    any(
        move |ws: WebSocketUpgrade,
              cookies: CookieJar,
              Query(query): Query<HashMap<String, String>>| {
            // Each connection owns its subscription and forwarding task.
            let endpoint = Arc::new(MonitorEndpoint::new(
                Arc::clone(&monitor),
                parse_init_limit(query.get("limit").map(String::as_str)),
            ));
            let authenticate = Arc::clone(&authenticate);
            let shutdown = shutdown.clone();
            async move { upgrade(ws, endpoint, authenticate, cookies, shutdown) }
        },
    )
}

fn parse_init_limit(limit: Option<&str>) -> usize {
    limit
        .and_then(|limit| limit.parse::<i32>().ok())
        .map(|limit| limit.clamp(0, MAX_HISTORY_SIZE as i32) as usize)
        .unwrap_or(MAX_HISTORY_SIZE)
}

struct MonitorEndpoint {
    monitor: Arc<MonitorManager>,
    limit: usize,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl MonitorEndpoint {
    fn new(monitor: Arc<MonitorManager>, limit: usize) -> Self {
        Self {
            monitor,
            limit,
            worker: Mutex::new(None),
        }
    }
}

impl Endpoint for MonitorEndpoint {
    async fn on_connect(&self, session: &WsSession) -> Result<(), EndpointError> {
        let (history, updates) = self.monitor.subscribe(self.limit);
        session.send(Packet::new("init", history))?;
        *self.worker.lock().await = Some(tokio::spawn(forward_updates(session.clone(), updates)));
        Ok(())
    }

    async fn on_disconnect(&self, _session: &WsSession) {
        if let Some(worker) = self.worker.lock().await.take() {
            worker.abort();
            let _ = worker.await;
        }
    }
}

async fn forward_updates(session: WsSession, mut updates: broadcast::Receiver<MonitorData>) {
    loop {
        let result = match updates.recv().await {
            Ok(data) => session.send(Packet::new("update", data)),
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

    use axum::{Router, extract::ws::Message};
    use futures_util::StreamExt;
    use serde_json::{Value, json};
    use tokio::{net::TcpListener, sync::mpsc, time::timeout};
    use tokio_tungstenite::connect_async;

    use crate::managers::ManagerContext;

    use super::*;

    fn monitor_manager() -> Arc<MonitorManager> {
        Arc::new(MonitorManager::new(ManagerContext::new(
            Weak::new(),
            CancellationToken::new(),
        )))
    }

    #[test]
    fn init_limit_matches_java_parsing_and_clamping() {
        for (input, expected) in [
            (None, MAX_HISTORY_SIZE),
            (Some(""), MAX_HISTORY_SIZE),
            (Some("  "), MAX_HISTORY_SIZE),
            (Some("invalid"), MAX_HISTORY_SIZE),
            (Some("1.5"), MAX_HISTORY_SIZE),
            (Some(" 1 "), MAX_HISTORY_SIZE),
            (Some("2147483648"), MAX_HISTORY_SIZE),
            (Some("-2147483649"), MAX_HISTORY_SIZE),
            (Some("0"), 0),
            (Some("-1"), 0),
            (Some("1"), 1),
            (Some("+2"), 2),
            (Some("201"), MAX_HISTORY_SIZE),
        ] {
            assert_eq!(parse_init_limit(input), expected, "limit: {input:?}");
        }
    }

    #[tokio::test]
    async fn route_sends_connect_then_limited_init_and_closes_on_shutdown() {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let shutdown = CancellationToken::new();
        let server_shutdown = shutdown.clone();
        let router = Router::new().route(
            "/monitor",
            route(monitor_manager(), Arc::new(|_| true), shutdown.clone()),
        );
        let server = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(server_shutdown.cancelled_owned())
                .await
        });

        for (query, expected) in [
            ("", MAX_HISTORY_SIZE),
            ("?limit=2", 2),
            ("?limit=-1", 0),
            ("?limit=bad", MAX_HISTORY_SIZE),
            ("?limit=201", MAX_HISTORY_SIZE),
        ] {
            let (mut socket, _) = connect_async(format!("ws://{address}/monitor{query}"))
                .await
                .unwrap();
            for kind in ["connect", "init"] {
                let message = timeout(Duration::from_secs(2), socket.next())
                    .await
                    .unwrap()
                    .unwrap()
                    .unwrap();
                let packet: Value = serde_json::from_str(message.to_text().unwrap()).unwrap();
                assert_eq!(packet["type"], kind);
                if kind == "init" {
                    assert_eq!(packet["data"].as_array().unwrap().len(), expected);
                }
            }
            socket.close(None).await.unwrap();
        }

        let (mut socket, _) = connect_async(format!("ws://{address}/monitor?limit=0"))
            .await
            .unwrap();
        for _ in 0..2 {
            timeout(Duration::from_secs(2), socket.next())
                .await
                .unwrap()
                .unwrap()
                .unwrap();
        }
        shutdown.cancel();
        let close = timeout(Duration::from_secs(2), socket.next())
            .await
            .unwrap()
            .unwrap()
            .unwrap();
        assert!(
            matches!(close, tokio_tungstenite::tungstenite::Message::Close(Some(frame)) if u16::from(frame.code) == 1001)
        );
        timeout(Duration::from_secs(2), server)
            .await
            .unwrap()
            .unwrap()
            .unwrap();
    }

    #[tokio::test]
    async fn forwards_update_packets_and_disconnect_releases_the_subscription() {
        let (sender, mut receiver) = mpsc::channel(4);
        let (close_sender, _close_receiver) = mpsc::unbounded_channel();
        let session = WsSession {
            sender,
            close_sender,
            closing: Arc::new(AtomicBool::new(false)),
        };
        let endpoint = MonitorEndpoint::new(monitor_manager(), 0);
        endpoint.on_connect(&session).await.unwrap();
        let Message::Text(init) = receiver.recv().await.unwrap() else {
            panic!("expected init packet");
        };
        assert_eq!(
            serde_json::from_str::<Value>(&init).unwrap(),
            json!({"type": "init", "data": []})
        );
        endpoint.on_disconnect(&session).await;
        assert_eq!(Arc::strong_count(&session.closing), 1);

        let (updates, subscription) = broadcast::channel(4);
        *endpoint.worker.lock().await =
            Some(tokio::spawn(forward_updates(session.clone(), subscription)));
        updates
            .send(MonitorData {
                cpu: 12.0,
                memory: 34.0,
                jvm_memory: 0.0,
                tps: 19.5,
                network_upload: 56.0,
                network_download: 78.0,
                disk_read: 90.0,
                disk_write: 10.0,
            })
            .unwrap();
        let Message::Text(update) = timeout(Duration::from_secs(1), receiver.recv())
            .await
            .unwrap()
            .unwrap()
        else {
            panic!("expected update packet");
        };
        assert_eq!(
            serde_json::from_str::<Value>(&update).unwrap(),
            json!({
                "type": "update",
                "data": {
                    "cpu": 12.0, "memory": 34.0, "jvmMemory": 0.0, "tps": 19.5,
                    "networkUpload": 56.0, "networkDownload": 78.0, "diskRead": 90.0, "diskWrite": 10.0,
                }
            })
        );
        endpoint.on_disconnect(&session).await;
        assert_eq!(updates.receiver_count(), 0);
        assert_eq!(Arc::strong_count(&session.closing), 1);
    }

    #[tokio::test]
    async fn closes_slow_consumers_when_updates_or_outgoing_messages_overflow() {
        for lagged in [false, true] {
            let (sender, _receiver) = mpsc::channel(1);
            let (close_sender, mut close_receiver) = mpsc::unbounded_channel();
            let session = WsSession {
                sender,
                close_sender,
                closing: Arc::new(AtomicBool::new(false)),
            };
            let (updates, subscription) = broadcast::channel(if lagged { 1 } else { 4 });
            for _ in 0..2 {
                updates.send(MonitorData::default()).unwrap();
            }
            timeout(
                Duration::from_secs(1),
                forward_updates(session, subscription),
            )
            .await
            .unwrap();
            assert_eq!(close_receiver.recv().await.unwrap().frame.code, 1013);
            assert_eq!(updates.receiver_count(), 0);
        }
    }
}
