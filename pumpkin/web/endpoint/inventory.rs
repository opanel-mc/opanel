use std::{sync::Arc, time::Duration};

use axum::{
    extract::{Path, WebSocketUpgrade},
    routing::{MethodRouter, any},
};
use axum_extra::extract::CookieJar;
use serde_json::Value;
use tokio::{
    sync::{Mutex, broadcast},
    task::JoinHandle,
    time::{Instant, MissedTickBehavior, interval_at},
};
use tokio_util::sync::CancellationToken;
use uuid::Uuid;

use crate::{
    opanel::OPanel,
    player::{
        Player, PlayerError,
        inventory::{InventoryError, InventoryUpdate, PlayerInventorySnapshot},
    },
};

use super::{
    Endpoint, EndpointError, Packet, SessionAuthenticator, WsSession, handle_endpoint_result,
    upgrade,
};

const POLL_INTERVAL: Duration = Duration::from_secs(1);

type InventoryAccess = Arc<
    dyn Fn(Uuid, Option<InventoryUpdate>) -> Result<PlayerInventorySnapshot, InventoryError>
        + Send
        + Sync,
>;

pub(super) fn route(
    opanel: Arc<OPanel>,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter {
    let server = Arc::clone(&opanel.context().server);
    let access: InventoryAccess = Arc::new(move |uuid, update| {
        // Resolve on every operation: a connected panel may outlive a game session.
        let player = Player::find(Arc::clone(&server), uuid)?;
        if let Some(update) = update {
            player.set_inventory_item(&update)?;
        }
        player.inventory()
    });
    inventory_route(access, authenticate, shutdown)
}

fn inventory_route(
    access: InventoryAccess,
    authenticate: SessionAuthenticator,
    shutdown: CancellationToken,
) -> MethodRouter {
    let (changes, _) = broadcast::channel(128);
    any(
        move |Path(uuid): Path<String>, ws: WebSocketUpgrade, cookies: CookieJar| {
            let endpoint = Arc::new(InventoryEndpoint::new(
                Uuid::parse_str(&uuid).ok(),
                Arc::clone(&access),
                changes.clone(),
            ));
            let authenticate = Arc::clone(&authenticate);
            let shutdown = shutdown.clone();
            async move { upgrade(ws, endpoint, authenticate, cookies, shutdown) }
        },
    )
}

struct InventoryEndpoint {
    inventory: Arc<InventorySession>,
    worker: Mutex<Option<JoinHandle<()>>>,
}

struct InventorySession {
    uuid: Option<Uuid>,
    access: InventoryAccess,
    changes: broadcast::Sender<Uuid>,
    last_hash: Mutex<Option<String>>,
}

impl InventoryEndpoint {
    fn new(uuid: Option<Uuid>, access: InventoryAccess, changes: broadcast::Sender<Uuid>) -> Self {
        Self {
            inventory: Arc::new(InventorySession {
                uuid,
                access,
                changes,
                last_hash: Mutex::new(None),
            }),
            worker: Mutex::new(None),
        }
    }
}

impl InventorySession {
    async fn refresh(
        &self,
        session: &WsSession,
        kind: &str,
        update: Option<InventoryUpdate>,
    ) -> Result<(), EndpointError> {
        let Some(uuid) = self.uuid else {
            session.close_with_error(400, 1008, "Invalid player UUID.");
            return Err(EndpointError::Closed);
        };
        // Serialize snapshot creation AND sending with fetches and edits. Otherwise a
        // slow fetch can overwrite a newer live update already sent to the same client.
        let mut last_hash = self.last_hash.lock().await;
        let changed = update.is_some();
        let access = Arc::clone(&self.access);
        let result = tokio::task::spawn_blocking(move || access(uuid, update)).await;
        let inventory = match result {
            Ok(Ok(inventory)) => inventory,
            Ok(Err(InventoryError::InvalidItem)) => {
                return session.send(Packet::new("error", 400));
            }
            Ok(Err(InventoryError::Player(PlayerError::NotFound))) => {
                session.close_with_error(404, 1008, "Player not found.");
                return Err(EndpointError::Closed);
            }
            result => {
                tracing::warn!(?result, %uuid, "Failed to access WebSocket player inventory");
                return Err(EndpointError::ServiceUnavailable(
                    "Failed to access player inventory.",
                ));
            }
        };
        if changed {
            // Send invalidations, not snapshots: other viewers always read the current
            // inventory, including changes that happened while their fetch was running.
            let _ = self.changes.send(uuid);
        }
        if kind == "init" || changed || last_hash.as_ref() != Some(&inventory.hash) {
            session.send(Packet::new(kind, &inventory))?;
            *last_hash = Some(inventory.hash);
        }
        Ok(())
    }
}

impl Endpoint for InventoryEndpoint {
    async fn on_connect(&self, session: &WsSession) -> Result<(), EndpointError> {
        let updates = self.inventory.changes.subscribe();
        self.inventory.refresh(session, "init", None).await?;
        *self.worker.lock().await = Some(tokio::spawn(watch_inventory(
            Arc::clone(&self.inventory),
            session.clone(),
            updates,
            POLL_INTERVAL,
        )));
        Ok(())
    }

    async fn on_packet(
        &self,
        session: &WsSession,
        packet: Packet<Value>,
    ) -> Result<(), EndpointError> {
        match packet.kind.as_str() {
            "fetch" => self.inventory.refresh(session, "init", None).await,
            "update" => {
                let Ok(update) = serde_json::from_value::<InventoryUpdate>(packet.data) else {
                    // Keep the connection open: the frontend fetches again to undo its
                    // optimistic edit when an item is rejected.
                    return session.send(Packet::new("error", 400));
                };
                self.inventory
                    .refresh(session, "update", Some(update))
                    .await
            }
            _ => Ok(()),
        }
    }

    async fn on_disconnect(&self, _session: &WsSession) {
        if let Some(worker) = self.worker.lock().await.take() {
            worker.abort();
            let _ = worker.await;
        }
    }
}

async fn watch_inventory(
    inventory: Arc<InventorySession>,
    session: WsSession,
    mut updates: broadcast::Receiver<Uuid>,
    poll_interval: Duration,
) {
    let mut ticks = interval_at(Instant::now() + poll_interval, poll_interval);
    ticks.set_missed_tick_behavior(MissedTickBehavior::Skip);
    loop {
        tokio::select! {
            _ = ticks.tick() => {}
            update = updates.recv() => match update {
                Ok(uuid) if Some(uuid) != inventory.uuid => continue,
                Err(broadcast::error::RecvError::Closed) => break,
                // If notifications were dropped, rereading still gives the latest state.
                Ok(_) | Err(broadcast::error::RecvError::Lagged(_)) => {}
            }
        }
        if handle_endpoint_result(inventory.refresh(&session, "update", None).await, &session) {
            break;
        }
    }
}

#[cfg(test)]
mod tests {
    use std::sync::{
        Mutex as StdMutex,
        atomic::{AtomicBool, AtomicI32, AtomicUsize, Ordering},
    };

    use axum::{Router, extract::ws::Message as WsMessage};
    use futures_util::{SinkExt, StreamExt};
    use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};
    use serde_json::json;
    use tokio::{
        net::TcpListener,
        sync::{mpsc, oneshot},
        time::timeout,
    };
    use tokio_tungstenite::{connect_async, tungstenite::Message};

    use super::*;

    type Socket = tokio_tungstenite::WebSocketStream<
        tokio_tungstenite::MaybeTlsStream<tokio::net::TcpStream>,
    >;

    fn snapshot(count: i32) -> PlayerInventorySnapshot {
        let mut item = NbtCompound::new();
        item.put_byte("Slot", 0);
        item.put_string("id", "minecraft:stone".into());
        item.put_int("count", count);
        let mut data = NbtCompound::new();
        data.put("Inventory", NbtTag::List(vec![NbtTag::Compound(item)]));
        PlayerInventorySnapshot::from_nbt(&data)
    }

    async fn packet(socket: &mut Socket) -> Value {
        let message = timeout(Duration::from_secs(2), socket.next())
            .await
            .unwrap()
            .unwrap()
            .unwrap();
        let Message::Text(text) = message else {
            panic!("expected packet, got {message:?}")
        };
        serde_json::from_str(&text).unwrap()
    }

    async fn assert_close(socket: &mut Socket, code: u16) {
        let message = timeout(Duration::from_secs(2), socket.next())
            .await
            .unwrap()
            .unwrap()
            .unwrap();
        assert!(matches!(message, Message::Close(Some(frame)) if u16::from(frame.code) == code));
    }

    async fn send(socket: &mut Socket, kind: &str, data: Value) {
        socket
            .send(Message::Text(
                json!({"type":kind,"data":data}).to_string().into(),
            ))
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn websocket_supports_scoped_updates_refetch_errors_and_shutdown() {
        let uuid = Uuid::from_u128(1);
        let other_uuid = Uuid::from_u128(2);
        let count = Arc::new(AtomicI32::new(1));
        let state = Arc::clone(&count);
        let access: InventoryAccess = Arc::new(move |id, update| {
            if id != uuid && id != other_uuid {
                return Err(PlayerError::NotFound.into());
            }
            if let Some(update) = update {
                if update.item.count == 999 {
                    return Err(InventoryError::InvalidItem);
                }
                state.store(update.item.count, Ordering::SeqCst);
            }
            Ok(snapshot(if id == uuid {
                state.load(Ordering::SeqCst)
            } else {
                10
            }))
        });
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let shutdown = CancellationToken::new();
        let stop = shutdown.clone();
        let router = Router::new()
            .route(
                "/inventory/{uuid}",
                inventory_route(Arc::clone(&access), Arc::new(|_| true), shutdown.clone()),
            )
            .route(
                "/unauthorized/{uuid}",
                inventory_route(access, Arc::new(|_| false), shutdown.clone()),
            );
        let server = tokio::spawn(async move {
            axum::serve(listener, router)
                .with_graceful_shutdown(stop.cancelled_owned())
                .await
                .unwrap();
        });

        let (mut denied, _) = connect_async(format!("ws://{address}/unauthorized/{uuid}"))
            .await
            .unwrap();
        assert_close(&mut denied, 1008).await;
        for (id, error) in [
            ("invalid".to_string(), 400),
            (Uuid::from_u128(3).to_string(), 404),
        ] {
            let (mut socket, _) = connect_async(format!("ws://{address}/inventory/{id}"))
                .await
                .unwrap();
            assert_eq!(packet(&mut socket).await["type"], "connect");
            assert_eq!(
                packet(&mut socket).await,
                json!({"type":"error","data":error})
            );
            assert_close(&mut socket, 1008).await;
        }

        let (mut first, _) = connect_async(format!("ws://{address}/inventory/{uuid}"))
            .await
            .unwrap();
        let (mut second, _) = connect_async(format!("ws://{address}/inventory/{uuid}"))
            .await
            .unwrap();
        let (mut other, _) = connect_async(format!("ws://{address}/inventory/{other_uuid}"))
            .await
            .unwrap();
        for socket in [&mut first, &mut second, &mut other] {
            assert_eq!(packet(socket).await["type"], "connect");
            let initial = packet(socket).await;
            assert_eq!(initial["type"], "init");
            assert_eq!(initial["data"]["main"]["size"], 36);
            assert_eq!(
                initial["data"]["equipments"]["items"]
                    .as_array()
                    .unwrap()
                    .len(),
                5
            );
            assert_eq!(
                initial["data"]["enderChest"]["items"]
                    .as_array()
                    .unwrap()
                    .len(),
                27
            );
        }
        for data in [
            Value::Null,
            json!({"inventoryType":"unknown","item":{"slot":0,"id":"minecraft:stone","count":1}}),
            json!({"inventoryType":"main","item":{"slot":-1,"id":"minecraft:stone","count":1}}),
            json!({"inventoryType":"main","item":{"slot":0,"id":"minecraft:stone","count":999}}),
        ] {
            send(&mut first, "update", data).await;
            assert_eq!(packet(&mut first).await, json!({"type":"error","data":400}));
            send(&mut first, "fetch", Value::Null).await;
            assert_eq!(
                packet(&mut first).await["data"]["main"]["items"][0]["count"],
                1
            );
        }
        send(
            &mut first,
            "update",
            json!({"inventoryType":"main","item":{"slot":0,"id":"minecraft:stone","count":7}}),
        )
        .await;
        for socket in [&mut first, &mut second] {
            let update = packet(socket).await;
            assert_eq!(update["type"], "update");
            assert_eq!(update["data"]["main"]["items"][0]["count"], 7);
        }
        // A fetch reads the current source instead of reusing the connection's snapshot.
        count.store(8, Ordering::SeqCst);
        send(&mut first, "fetch", Value::Null).await;
        assert_eq!(
            packet(&mut first).await["data"]["main"]["items"][0]["count"],
            8
        );
        assert!(
            timeout(Duration::from_millis(50), other.next())
                .await
                .is_err()
        );
        shutdown.cancel();
        for socket in [&mut first, &mut second, &mut other] {
            assert_close(socket, 1001).await;
        }
        timeout(Duration::from_secs(2), server)
            .await
            .unwrap()
            .unwrap();
    }

    fn session() -> (
        WsSession,
        mpsc::Receiver<WsMessage>,
        mpsc::UnboundedReceiver<super::super::CloseRequest>,
    ) {
        let (sender, receiver) = mpsc::channel(16);
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

    async fn receive(receiver: &mut mpsc::Receiver<WsMessage>) -> Value {
        let message = timeout(Duration::from_secs(2), receiver.recv())
            .await
            .unwrap()
            .unwrap();
        let WsMessage::Text(text) = message else {
            panic!("expected JSON packet")
        };
        serde_json::from_str(&text).unwrap()
    }

    #[tokio::test]
    async fn polling_sends_only_changes_and_disconnect_releases_worker() {
        let count = Arc::new(AtomicI32::new(1));
        let current = Arc::clone(&count);
        let access: InventoryAccess =
            Arc::new(move |_, _| Ok(snapshot(current.load(Ordering::SeqCst))));
        let (changes, _) = broadcast::channel(8);
        let endpoint = InventoryEndpoint::new(Some(Uuid::from_u128(1)), access, changes.clone());
        let (session, mut receiver, _close) = session();
        endpoint
            .inventory
            .refresh(&session, "init", None)
            .await
            .unwrap();
        assert_eq!(receive(&mut receiver).await["type"], "init");
        *endpoint.worker.lock().await = Some(tokio::spawn(watch_inventory(
            Arc::clone(&endpoint.inventory),
            session.clone(),
            changes.subscribe(),
            Duration::from_millis(5),
        )));
        assert!(
            timeout(Duration::from_millis(40), receiver.recv())
                .await
                .is_err()
        );
        count.store(2, Ordering::SeqCst);
        let update = receive(&mut receiver).await;
        assert_eq!(update["type"], "update");
        assert_eq!(update["data"]["main"]["items"][0]["count"], 2);
        assert!(
            timeout(Duration::from_millis(40), receiver.recv())
                .await
                .is_err()
        );
        endpoint.on_disconnect(&session).await;
        assert_eq!(changes.receiver_count(), 0);
        assert_eq!(Arc::strong_count(&session.closing), 1);
        assert!(endpoint.worker.lock().await.is_none());
    }

    #[tokio::test]
    async fn slow_fetch_cannot_overwrite_a_newer_update() {
        let count = Arc::new(AtomicI32::new(1));
        let current = Arc::clone(&count);
        let calls = Arc::new(AtomicUsize::new(0));
        let called = Arc::clone(&calls);
        let (started, start) = oneshot::channel();
        let started = StdMutex::new(Some(started));
        let (release, gate) = std::sync::mpsc::channel();
        let gate = StdMutex::new(gate);
        let access: InventoryAccess = Arc::new(move |_, _| {
            let captured = current.load(Ordering::SeqCst);
            if called.fetch_add(1, Ordering::SeqCst) == 0 {
                started.lock().unwrap().take().unwrap().send(()).unwrap();
                gate.lock()
                    .unwrap()
                    .recv_timeout(Duration::from_secs(2))
                    .unwrap();
            }
            Ok(snapshot(captured))
        });
        let (changes, _) = broadcast::channel(8);
        let endpoint = InventoryEndpoint::new(Some(Uuid::from_u128(1)), access, changes);
        let (session, mut receiver, _close) = session();
        let inventory = Arc::clone(&endpoint.inventory);
        let fetch_session = session.clone();
        let fetch =
            tokio::spawn(async move { inventory.refresh(&fetch_session, "init", None).await });
        timeout(Duration::from_secs(2), start)
            .await
            .unwrap()
            .unwrap();
        count.store(2, Ordering::SeqCst);
        let inventory = Arc::clone(&endpoint.inventory);
        let live = tokio::spawn(async move { inventory.refresh(&session, "update", None).await });
        assert!(
            timeout(Duration::from_millis(40), receiver.recv())
                .await
                .is_err()
        );
        assert_eq!(
            calls.load(Ordering::SeqCst),
            1,
            "live snapshot must wait for fetch"
        );
        release.send(()).unwrap();
        let initial = receive(&mut receiver).await;
        assert_eq!(initial["type"], "init");
        assert_eq!(initial["data"]["main"]["items"][0]["count"], 1);
        let update = receive(&mut receiver).await;
        assert_eq!(update["type"], "update");
        assert_eq!(update["data"]["main"]["items"][0]["count"], 2);
        fetch.await.unwrap().unwrap();
        live.await.unwrap().unwrap();
    }
}
