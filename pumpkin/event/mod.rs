use std::{
    collections::HashMap,
    error::Error,
    future::Future,
    pin::Pin,
    sync::{Arc, Mutex, PoisonError},
    time::Duration,
};

use arc_swap::ArcSwap;
use pumpkin::plugin::{
    HandlerMap, Payload,
    player::{PlayerGamemodeChangeEvent, PlayerJoinEvent, PlayerLeaveEvent, PlayerMoveEvent},
};
use pumpkin_util::{GameMode, math::vector3::Vector3};
use serde_json::{Value, json};
use tokio::{sync::Mutex as AsyncMutex, sync::broadcast, task::JoinHandle, time::Instant};
use uuid::Uuid;

use crate::managers::{Manager, ManagerContext};

mod player;

const MOVE_BROADCAST_INTERVAL: Duration = Duration::from_secs(1);
const MAX_PENDING_EVENTS: usize = 1024;

#[derive(Debug, Clone, PartialEq)]
pub(crate) enum PlayerEvent {
    Join(Value),
    Leave(Value),
    Move(Vec<Value>),
    GameModeChange(Value),
}

/// Pumpkin events are registered once; dropping a receiver unsubscribes a consumer.
pub(crate) struct EventManager {
    context: ManagerContext,
    state: Arc<Mutex<EventState>>,
    worker: AsyncMutex<Option<JoinHandle<()>>>,
}

impl EventManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        Self {
            context,
            state: Arc::new(Mutex::new(EventState::new())),
            worker: AsyncMutex::new(None),
        }
    }

    pub(crate) fn subscribe(&self) -> broadcast::Receiver<PlayerEvent> {
        self.state
            .lock()
            .unwrap_or_else(PoisonError::into_inner)
            .updates
            .subscribe()
    }

    pub(crate) fn join_time(&self, uuid: Uuid) -> Option<u128> {
        self.state
            .lock()
            .unwrap_or_else(PoisonError::into_inner)
            .join_times
            .get(&uuid)
            .copied()
    }
}

struct EventState {
    join_times: HashMap<Uuid, u128>,
    pending_moves: HashMap<Uuid, Value>,
    updates: broadcast::Sender<PlayerEvent>,
}

impl EventState {
    fn new() -> Self {
        Self {
            join_times: HashMap::new(),
            pending_moves: HashMap::new(),
            updates: broadcast::channel(MAX_PENDING_EVENTS).0,
        }
    }

    fn join(&mut self, uuid: Uuid, mut data: Value, time: u128) {
        self.join_times.insert(uuid, time);
        data["joinTime"] = json!(time);
        let _ = self.updates.send(PlayerEvent::Join(data));
    }

    fn leave(&mut self, uuid: Uuid, mut data: Value) {
        self.join_times.remove(&uuid);
        self.pending_moves.remove(&uuid);
        data["isOnline"] = json!(false);
        data["joinTime"] = Value::Null;
        let _ = self.updates.send(PlayerEvent::Leave(data));
    }

    fn game_mode_change(&self, uuid: Uuid, mut data: Value, mode: GameMode) {
        data["gamemode"] = json!(mode.name());
        data["joinTime"] = json!(self.join_times.get(&uuid));
        let _ = self.updates.send(PlayerEvent::GameModeChange(data));
    }

    fn record_move(&mut self, uuid: Uuid, name: &str, from: Vector3<f64>, to: Vector3<f64>) {
        if from == to {
            return;
        }
        self.pending_moves.insert(
            uuid,
            json!({
                "uuid": uuid,
                "name": name,
                "position": {"x": to.x, "y": to.y, "z": to.z},
            }),
        );
    }

    fn flush_moves(&mut self) {
        if !self.pending_moves.is_empty() {
            let moves = self.pending_moves.drain().map(|(_, data)| data).collect();
            let _ = self.updates.send(PlayerEvent::Move(moves));
        }
    }
}

impl Manager for EventManager {
    fn name(&self) -> &'static str {
        "event"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }

    fn start(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            let mut worker = self.worker.lock().await;
            if worker.is_some() {
                return Ok(());
            }
            let context = self.opanel()?.context();
            let shutdown = self.shutdown_token();
            player::register(&context, Arc::clone(&self.state), shutdown.clone());

            let state = Arc::clone(&self.state);
            *worker = Some(tokio::spawn(async move {
                let mut interval = tokio::time::interval_at(
                    Instant::now() + MOVE_BROADCAST_INTERVAL,
                    MOVE_BROADCAST_INTERVAL,
                );
                interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Delay);
                loop {
                    tokio::select! {
                        biased;
                        () = shutdown.cancelled() => break,
                        _ = interval.tick() => {
                            state.lock().unwrap_or_else(PoisonError::into_inner).flush_moves();
                        }
                    }
                }
            }));
            Ok(())
        })
    }

    fn shutdown(
        &self,
    ) -> Pin<Box<dyn Future<Output = Result<(), Box<dyn Error + Send + Sync>>> + Send + '_>> {
        Box::pin(async move {
            let context = self.opanel()?.context();
            unregister(&context.handlers, &context.get_metadata().name);
            if let Some(worker) = self.worker.lock().await.take() {
                worker.await?;
            }
            let mut state = self.state.lock().unwrap_or_else(PoisonError::into_inner);
            state.join_times.clear();
            state.pending_moves.clear();
            Ok(())
        })
    }
}

fn unregister(handlers: &ArcSwap<HandlerMap>, source: &str) {
    handlers.rcu(|handlers| {
        let mut handlers = (**handlers).clone();
        for name in [
            PlayerJoinEvent::get_name_static(),
            PlayerLeaveEvent::get_name_static(),
            PlayerMoveEvent::get_name_static(),
            PlayerGamemodeChangeEvent::get_name_static(),
        ] {
            if let Some(listeners) = handlers.get_mut(name) {
                listeners.retain(|listener| listener.source() != Some(source));
                if listeners.is_empty() {
                    handlers.remove(name);
                }
            }
        }
        Arc::new(handlers)
    });
}

#[cfg(test)]
mod tests {
    use std::sync::Weak;

    use pumpkin::plugin::{EventHandler, EventPriority, TypedEventHandler};
    use tokio_util::sync::CancellationToken;

    use super::*;

    #[test]
    fn join_times_survive_without_subscribers_and_reset_on_rejoin() {
        let events = EventManager::new(ManagerContext::new(Weak::new(), CancellationToken::new()));
        let uuid = Uuid::from_u128(1);
        let data = json!({"uuid": uuid, "isOnline": true});
        events.state.lock().unwrap().join(uuid, data.clone(), 100);
        assert_eq!(events.join_time(uuid), Some(100));

        let mut first = events.subscribe();
        let mut second = events.subscribe();
        events.state.lock().unwrap().leave(uuid, data.clone());
        assert_eq!(events.join_time(uuid), None);
        let expected = PlayerEvent::Leave(json!({
            "uuid": uuid, "isOnline": false, "joinTime": null,
        }));
        assert_eq!(first.try_recv().unwrap(), expected);
        assert_eq!(second.try_recv().unwrap(), expected);
        drop(first);

        events.state.lock().unwrap().join(uuid, data, 200);
        assert_eq!(events.join_time(uuid), Some(200));
        assert_eq!(
            second.try_recv().unwrap(),
            PlayerEvent::Join(json!({
                "uuid": uuid, "isOnline": true, "joinTime": 200,
            }))
        );
        assert_eq!(events.state.lock().unwrap().updates.receiver_count(), 1);
    }

    #[test]
    fn gamemode_updates_preserve_each_players_join_time_and_allow_missing_times() {
        let mut state = EventState::new();
        let first = Uuid::from_u128(1);
        let second = Uuid::from_u128(2);
        let untracked = Uuid::from_u128(3);
        state.join(first, json!({"uuid": first}), 100);
        state.join(second, json!({"uuid": second}), 200);
        let mut updates = state.updates.subscribe();

        for (uuid, join_time) in [
            (first, Some(100u128)),
            (second, Some(200)),
            (untracked, None),
        ] {
            state.game_mode_change(
                uuid,
                json!({
                    "uuid": uuid, "name": "Alex", "isOnline": true,
                    "gamemode": "survival", "joinTime": null,
                }),
                GameMode::Creative,
            );
            assert_eq!(
                updates.try_recv().unwrap(),
                PlayerEvent::GameModeChange(json!({
                    "uuid": uuid, "name": "Alex", "isOnline": true,
                    "gamemode": "creative", "joinTime": join_time,
                }))
            );
            assert_eq!(state.join_times.get(&uuid).copied(), join_time);
        }
    }

    #[test]
    fn moves_keep_latest_position_per_player_and_leave_discards_pending_moves() {
        let mut state = EventState::new();
        let mut updates = state.updates.subscribe();
        let first = Uuid::from_u128(1);
        let second = Uuid::from_u128(2);
        let movement = |uuid, x| {
            json!({
                "uuid": uuid, "name": "player", "position": {"x": x, "y": 64.0, "z": 0.0},
            })
        };

        let position = |x| Vector3::new(x, 64.0, 0.0);
        state.record_move(first, "player", position(0.0), position(1.0));
        state.record_move(first, "player", position(1.0), position(2.0));
        state.record_move(second, "player", position(0.0), position(3.0));
        assert!(updates.try_recv().is_err());
        state.flush_moves();
        let PlayerEvent::Move(mut moves) = updates.try_recv().unwrap() else {
            panic!("expected a move batch");
        };
        moves.sort_by_key(|data| data["uuid"].as_str().unwrap().to_string());
        assert_eq!(moves, vec![movement(first, 2.0), movement(second, 3.0)]);
        state.flush_moves();
        assert!(updates.try_recv().is_err());

        state.record_move(first, "player", position(2.0), position(4.0));
        state.record_move(second, "player", position(3.0), position(5.0));
        state.leave(first, json!({"uuid": first, "isOnline": true}));
        assert!(matches!(updates.try_recv().unwrap(), PlayerEvent::Leave(_)));
        state.flush_moves();
        assert_eq!(
            updates.try_recv().unwrap(),
            PlayerEvent::Move(vec![movement(second, 5.0)])
        );
    }

    #[test]
    fn stationary_positions_do_not_enqueue_or_repeat_moves() {
        let mut state = EventState::new();
        let mut updates = state.updates.subscribe();
        let uuid = Uuid::from_u128(1);
        let origin = Vector3::new(0.0, 64.0, 0.0);
        state.record_move(uuid, "player", origin, origin);
        assert!(state.pending_moves.is_empty());
        state.flush_moves();
        assert!(matches!(
            updates.try_recv(),
            Err(broadcast::error::TryRecvError::Empty)
        ));

        for to in [
            Vector3::new(0.01, 64.0, 0.0),
            Vector3::new(0.0, 64.01, 0.0),
            Vector3::new(0.0, 64.0, 0.01),
        ] {
            state.record_move(uuid, "player", origin, to);
            state.record_move(uuid, "player", to, to);
            state.flush_moves();
            assert!(
                matches!(updates.try_recv().unwrap(), PlayerEvent::Move(moves) if moves.len() == 1)
            );
            state.record_move(uuid, "player", to, to);
            state.flush_moves();
            assert!(matches!(
                updates.try_recv(),
                Err(broadcast::error::TryRecvError::Empty)
            ));
        }
    }

    #[test]
    fn unregister_removes_opanel_handlers_and_preserves_other_plugins() {
        struct Listener;
        impl EventHandler<PlayerJoinEvent> for Listener {}

        let mut handlers = HandlerMap::new();
        for source in [Some("OPanel"), Some("OtherPlugin"), None] {
            handlers
                .entry(PlayerJoinEvent::get_name_static())
                .or_default()
                .push(Arc::new(TypedEventHandler::<PlayerJoinEvent, _> {
                    handler: Arc::new(Listener),
                    priority: EventPriority::Normal,
                    blocking: false,
                    source: source.map(str::to_string),
                    _phantom: std::marker::PhantomData,
                }));
        }
        let handlers = ArcSwap::from_pointee(handlers);
        unregister(&handlers, "OPanel");
        let map = handlers.load();
        let remaining = &map[PlayerJoinEvent::get_name_static()];
        assert_eq!(remaining.len(), 2);
        assert_eq!(remaining[0].source(), Some("OtherPlugin"));
        assert_eq!(remaining[1].source(), None);
        drop(map);
        unregister(&handlers, "OtherPlugin");
        assert_eq!(handlers.load()[PlayerJoinEvent::get_name_static()].len(), 1);
    }
}
