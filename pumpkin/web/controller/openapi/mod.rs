use std::sync::Arc;

use axum::Router;

use crate::{opanel::OPanel, web::response};

mod info;
mod logs;
mod monitor;
mod players;
mod plugins;

pub(super) fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .merge(info::router())
        .nest("/logs", logs::router())
        .merge(monitor::router())
        .nest("/players", players::router())
        .nest("/plugins", plugins::router())
        .fallback(response::not_found)
}
