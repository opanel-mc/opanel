use std::sync::Arc;

use axum::{
    extract::WebSocketUpgrade,
    routing::{MethodRouter, any},
};
use axum_extra::extract::CookieJar;
use serde_json::{Value, json};
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
};
use tokio_util::sync::CancellationToken;
use uuid::Uuid;

use crate::{
    event::{EventManager, PlayerEvent},
    managers::Manager,
    player::Player,
};

use super::{
    Endpoint, EndpointError, Packet, SessionAuthenticator, WsSession, handle_endpoint_result,
    upgrade,
};

pub(super) fn route(
    events: Arc<EventManager>,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter {
    any(move |ws: WebSocketUpgrade, cookies: CookieJar| {
        let endpoint = Arc::new(PlayersEndpoint::new(Arc::clone(&events)));
        let authenticate = Arc::clone(&authenticate);
        let shutdown = shutdown.clone();
        async move { upgrade(ws, endpoint, authenticate, cookies, shutdown) }
    })
}

struct PlayersEndpoint {
    events: Arc<EventManager>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

impl PlayersEndpoint {
    fn new(events: Arc<EventManager>) -> Self {
        Self {
            events,
            worker: Mutex::new(None),
        }
    }

    async fn send_player_list(&self, session: &WsSession) -> Result<(), EndpointError> {
        let opanel = self
            .events
            .opanel()
            .map_err(|_| EndpointError::ServiceUnavailable("OPanel is no longer available."))?;
        let server = Arc::clone(&opanel.context().server);
        let events = Arc::clone(&self.events);
        let players = tokio::task::spawn_blocking(move || {
            let mut players = Player::list(server)?;
            for player in &mut players {
                if player["isOnline"] == true
                    && let Some(uuid) = player["uuid"].as_str()
                    && let Ok(uuid) = Uuid::parse_str(uuid)
                {
                    player["joinTime"] = json!(events.join_time(uuid));
                }
            }
            Ok::<_, crate::player::PlayerError>(players)
        })
        .await;
        let players = match players {
            Ok(Ok(players)) => players,
            result => {
                tracing::warn!(?result, "Failed to read WebSocket player list");
                return Err(EndpointError::ServiceUnavailable(
                    "Failed to read player list.",
                ));
            }
        };
        session.send(Packet::new("init", players))
    }
}

impl Endpoint for PlayersEndpoint {
    async fn on_connect(&self, session: &WsSession) -> Result<(), EndpointError> {
        let updates = self.events.subscribe();
        self.send_player_list(session).await?;
        *self.worker.lock().await = Some(tokio::spawn(forward_events(session.clone(), updates)));
        Ok(())
    }

    async fn on_packet(
        &self,
        session: &WsSession,
        packet: Packet<Value>,
    ) -> Result<(), EndpointError> {
        if packet.kind == "fetch" {
            self.send_player_list(session).await?;
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

async fn forward_events(session: WsSession, mut updates: broadcast::Receiver<PlayerEvent>) {
    loop {
        let result = match updates.recv().await {
            Ok(PlayerEvent::Join(data)) => session.send(Packet::new("join", data)),
            Ok(PlayerEvent::Leave(data)) => session.send(Packet::new("leave", data)),
            Ok(PlayerEvent::Move(data)) => session.send(Packet::new("move", data)),
            Ok(PlayerEvent::GameModeChange(data)) => {
                session.send(Packet::new("gamemode-change", data))
            }
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

    use axum::extract::ws::Message;
    use tokio::{sync::mpsc, time::timeout};

    use crate::managers::ManagerContext;

    use super::*;

    fn session(
        capacity: usize,
    ) -> (
        WsSession,
        mpsc::Receiver<Message>,
        mpsc::UnboundedReceiver<super::super::CloseRequest>,
    ) {
        let (sender, receiver) = mpsc::channel(capacity);
        let (close_sender, close_receiver) = mpsc::unbounded_channel();
        (
            WsSession {
                sender,
                close_sender,
                closing: Arc::new(AtomicBool::new(false)),
            },
            receiver,
            close_receiver,
        )
    }

    async fn receive(receiver: &mut mpsc::Receiver<Message>) -> Value {
        let message = timeout(Duration::from_secs(1), receiver.recv())
            .await
            .unwrap()
            .unwrap();
        let Message::Text(text) = message else {
            panic!("expected a JSON packet");
        };
        serde_json::from_str(&text).unwrap()
    }

    #[tokio::test]
    async fn player_packets_reach_independent_connections_and_disconnect_releases_subscription() {
        let events = Arc::new(EventManager::new(ManagerContext::new(
            Weak::new(),
            CancellationToken::new(),
        )));
        let first = PlayersEndpoint::new(Arc::clone(&events));
        let second = PlayersEndpoint::new(events);
        let (first_session, mut first_receiver, _first_close) = session(8);
        let (second_session, mut second_receiver, _second_close) = session(8);
        let (updates, _) = broadcast::channel(8);
        *first.worker.lock().await = Some(tokio::spawn(forward_events(
            first_session.clone(),
            updates.subscribe(),
        )));
        *second.worker.lock().await = Some(tokio::spawn(forward_events(
            second_session.clone(),
            updates.subscribe(),
        )));

        let player = json!({"uuid": Uuid::from_u128(1), "name": "Alex", "isOnline": true, "gamemode": "creative"});
        let movement = json!({"uuid": Uuid::from_u128(1), "name": "Alex", "position": {"x": 1, "y": 64, "z": 2}});
        for (event, kind, data) in [
            (PlayerEvent::Join(player.clone()), "join", player.clone()),
            (
                PlayerEvent::Move(vec![movement.clone()]),
                "move",
                json!([movement]),
            ),
            (
                PlayerEvent::GameModeChange(player.clone()),
                "gamemode-change",
                player.clone(),
            ),
            (PlayerEvent::Leave(player.clone()), "leave", player.clone()),
        ] {
            updates.send(event).unwrap();
            let expected = json!({"type": kind, "data": data});
            assert_eq!(receive(&mut first_receiver).await, expected);
            assert_eq!(receive(&mut second_receiver).await, expected);
        }
        first.on_disconnect(&first_session).await;
        assert_eq!(updates.receiver_count(), 1);
        assert_eq!(Arc::strong_count(&first_session.closing), 1);
        updates.send(PlayerEvent::Join(player.clone())).unwrap();
        assert_eq!(
            receive(&mut second_receiver).await,
            json!({"type": "join", "data": player})
        );
        assert!(first_receiver.try_recv().is_err());
        second.on_disconnect(&second_session).await;
        assert_eq!(updates.receiver_count(), 0);
        assert!(first.worker.lock().await.is_none());
        assert!(second.worker.lock().await.is_none());
    }

    #[tokio::test]
    async fn player_forwarding_closes_slow_or_lagging_consumers() {
        for lagged in [false, true] {
            let (session, _receiver, mut close_receiver) = session(1);
            let (updates, subscription) = broadcast::channel(2);
            for _ in 0..if lagged { 3 } else { 2 } {
                updates
                    .send(PlayerEvent::Join(json!({"name": "Alex"})))
                    .unwrap();
            }
            timeout(
                Duration::from_secs(1),
                forward_events(session, subscription),
            )
            .await
            .unwrap();
            assert_eq!(close_receiver.recv().await.unwrap().frame.code, 1013);
        }
    }
}
