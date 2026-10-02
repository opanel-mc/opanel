use std::sync::{Arc, Mutex, PoisonError};

use pumpkin::{
    entity::player::Player as PumpkinPlayer,
    plugin::{
        BoxFuture, Context, EventHandler, EventPriority,
        player::{PlayerGamemodeChangeEvent, PlayerJoinEvent, PlayerLeaveEvent, PlayerMoveEvent},
    },
    server::Server,
};
use serde_json::Value;
use tokio_util::sync::CancellationToken;

use crate::{player::Player, utils::time::unix_time_millis};

use super::EventState;

pub(super) fn register(
    context: &Context,
    state: Arc<Mutex<EventState>>,
    shutdown: CancellationToken,
) {
    let listener = Arc::new(PlayerListener { state, shutdown });
    // Non-blocking observers run after all handlers that can cancel or modify events.
    context.register_event::<PlayerJoinEvent, _>(
        Arc::clone(&listener),
        EventPriority::Normal,
        false,
    );
    context.register_event::<PlayerLeaveEvent, _>(
        Arc::clone(&listener),
        EventPriority::Normal,
        false,
    );
    context.register_event::<PlayerMoveEvent, _>(
        Arc::clone(&listener),
        EventPriority::Normal,
        false,
    );
    context.register_event::<PlayerGamemodeChangeEvent, _>(listener, EventPriority::Normal, false);
}

struct PlayerListener {
    state: Arc<Mutex<EventState>>,
    shutdown: CancellationToken,
}

fn snapshot(server: &Arc<Server>, player: &Arc<PumpkinPlayer>) -> Option<Value> {
    // A leaving player may already have been removed from the server's player list.
    match Player::from_online(Arc::clone(server), Arc::clone(player)).snapshot() {
        Ok(data) => Some(data),
        Err(error) => {
            tracing::warn!(%error, uuid = %player.gameprofile.id, "Failed to serialize player event");
            None
        }
    }
}

impl EventHandler<PlayerJoinEvent> for PlayerListener {
    fn handle<'a>(
        &'a self,
        server: &'a Arc<Server>,
        event: &'a PlayerJoinEvent,
    ) -> BoxFuture<'a, ()> {
        Box::pin(async move {
            // Cancelling join/leave only suppresses the chat message, not the lifecycle change.
            if !self.shutdown.is_cancelled()
                && let Some(data) = snapshot(server, &event.player)
            {
                self.state
                    .lock()
                    .unwrap_or_else(PoisonError::into_inner)
                    .join(event.player.gameprofile.id, data, unix_time_millis());
            }
        })
    }
}

impl EventHandler<PlayerLeaveEvent> for PlayerListener {
    fn handle<'a>(
        &'a self,
        server: &'a Arc<Server>,
        event: &'a PlayerLeaveEvent,
    ) -> BoxFuture<'a, ()> {
        Box::pin(async move {
            if !self.shutdown.is_cancelled()
                && let Some(data) = snapshot(server, &event.player)
            {
                self.state
                    .lock()
                    .unwrap_or_else(PoisonError::into_inner)
                    .leave(event.player.gameprofile.id, data);
            }
        })
    }
}

impl EventHandler<PlayerMoveEvent> for PlayerListener {
    fn handle<'a>(
        &'a self,
        _server: &'a Arc<Server>,
        event: &'a PlayerMoveEvent,
    ) -> BoxFuture<'a, ()> {
        Box::pin(async move {
            if self.shutdown.is_cancelled() || event.cancelled {
                return;
            }
            self.state
                .lock()
                .unwrap_or_else(PoisonError::into_inner)
                .record_move(
                    event.player.gameprofile.id,
                    &event.player.gameprofile.name,
                    event.from,
                    event.to,
                );
        })
    }
}

impl EventHandler<PlayerGamemodeChangeEvent> for PlayerListener {
    fn handle<'a>(
        &'a self,
        server: &'a Arc<Server>,
        event: &'a PlayerGamemodeChangeEvent,
    ) -> BoxFuture<'a, ()> {
        Box::pin(async move {
            if !self.shutdown.is_cancelled()
                && !event.cancelled
                && let Some(data) = snapshot(server, &event.player)
            {
                self.state
                    .lock()
                    .unwrap_or_else(PoisonError::into_inner)
                    .game_mode_change(event.player.gameprofile.id, data, event.new_gamemode);
            }
        })
    }
}
