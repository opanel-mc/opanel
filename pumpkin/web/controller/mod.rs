use std::sync::Arc;

use axum::{
    Router,
    extract::State,
    routing::{MethodRouter, any, delete, get, post},
};

use crate::opanel::OPanel;

use super::response::{self, ApiError};

mod assets;
mod auth;
mod banned_ips;
mod control;
mod download;
mod gamerules;
mod icon;
mod info;
mod logs;
mod map;
mod mcp;
mod monitor;
mod oidc;
mod open_api;
mod openapi;
mod players;
mod plugins;
mod saves;
mod security;
mod tasks;
mod terminal;
mod version;
mod whitelist;

pub fn router() -> Router<Arc<OPanel>> {
    Router::new()
        .nest("/api", api_router())
        .merge(assets::router())
        .nest("/file", download::router())
        .nest("/open-api", openapi::router())
        .route("/api", any(response::not_found))
        .route("/file", any(response::not_found))
        .route("/open-api", any(response::not_found))
}

fn api_router() -> Router<Arc<OPanel>> {
    Router::new()
        .nest("/auth", auth::router().nest("/oidc", oidc::router()))
        .nest("/banned-ips", banned_ips::router())
        .nest("/control", control::router())
        .nest("/gamerules", gamerules::router())
        .nest("/icon", icon::router())
        .nest("/info", info::router())
        .nest("/logs", logs::router())
        .nest("/map", map::router())
        .nest("/monitor", monitor::router())
        .nest("/players", players::router())
        .nest("/saves", saves::router())
        .nest("/plugins", plugins::router())
        .nest("/terminal", terminal::router())
        .nest("/whitelist", whitelist::router())
        .nest("/tasks", tasks::router())
        .nest("/mcp", mcp::router())
        .nest("/open-api", open_api::router())
        .nest("/security", security::router())
        .nest("/version", version::router())
        .fallback(response::not_found)
}

async fn not_implemented(State(_opanel): State<Arc<OPanel>>) -> ApiError {
    ApiError::not_implemented()
}

fn with_method_fallback(router: MethodRouter<Arc<OPanel>>) -> MethodRouter<Arc<OPanel>> {
    router.fallback(response::method_not_allowed)
}

pub(super) fn get_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(get(not_implemented))
}

pub(super) fn post_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(post(not_implemented))
}

pub(super) fn delete_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(delete(not_implemented))
}

pub(super) fn get_post_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(get(not_implemented).post(not_implemented))
}

pub(super) fn get_delete_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(get(not_implemented).delete(not_implemented))
}

pub(super) fn get_post_delete_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(
        get(not_implemented)
            .post(not_implemented)
            .delete(not_implemented),
    )
}

pub(super) fn get_post_patch_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(
        get(not_implemented)
            .post(not_implemented)
            .patch(not_implemented),
    )
}

pub(super) fn get_post_patch_delete_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(
        get(not_implemented)
            .post(not_implemented)
            .patch(not_implemented)
            .delete(not_implemented),
    )
}

pub(super) fn post_patch_delete_route() -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(
        post(not_implemented)
            .patch(not_implemented)
            .delete(not_implemented),
    )
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use axum::Router;

    use crate::opanel::OPanel;

    #[test]
    fn controller_router_builds_without_route_conflicts() {
        let _: Router<Arc<OPanel>> = Router::new()
            .merge(super::router())
            .fallback(|| async { "frontend fallback" });
    }
}
