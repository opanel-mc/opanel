use std::{convert::Infallible, sync::Arc};

use axum::{
    Extension, Router,
    http::Method,
    middleware,
    routing::{MethodRouter, any, get, post},
};

use crate::opanel::OPanel;

use super::{
    middleware::{AuthRouteRegistry, AuthRouteRole, authorize},
    response,
};

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
    let managed_router = Router::new()
        .nest("/assets", assets_router())
        .nest("/file", file_router())
        .nest("/api", api_router())
        .route("/api", any(response::not_found))
        .route("/file", any(response::not_found))
        .route_layer(middleware::from_fn(authorize))
        .route_layer(Extension(managed_auth_registry()));

    Router::new()
        .merge(managed_router)
        .nest("/open-api", open_api_router())
        .route("/open-api", any(response::not_found))
}

fn with_method_fallback(router: MethodRouter<Arc<OPanel>>) -> MethodRouter<Arc<OPanel>> {
    router.fallback(response::method_not_allowed)
}

fn with_role(router: MethodRouter<Arc<OPanel>>, role: AuthRouteRole) -> MethodRouter<Arc<OPanel>> {
    with_method_fallback(role_layer(router, role))
}

fn role_layer(router: MethodRouter<Arc<OPanel>>, role: AuthRouteRole) -> MethodRouter<Arc<OPanel>> {
    router.layer::<_, Infallible>(Extension(role))
}

fn with_role_router(router: Router<Arc<OPanel>>, role: AuthRouteRole) -> Router<Arc<OPanel>> {
    router.route_layer(Extension(role))
}

fn managed_auth_registry() -> AuthRouteRegistry {
    use AuthRouteRole::{PanelOrMcp, PanelSession, Public};

    let mut registry = AuthRouteRegistry::default();

    registry.register(
        [Method::GET],
        [
            "/assets/{name}",
            "/api/auth/oidc/login",
            "/api/auth/oidc/callback",
            "/api/auth/oidc/config",
            "/api/icon",
            "/api/icon/",
        ],
        Public,
    );
    registry.register(
        [Method::GET, Method::POST],
        ["/api/auth", "/api/auth/"],
        Public,
    );
    registry.register(
        [Method::POST],
        [
            "/api/auth/check",
            "/api/auth/logout",
            "/api/auth/oidc/bind-user",
        ],
        Public,
    );

    registry.register(
        [Method::GET, Method::POST, Method::DELETE],
        ["/api/auth/oidc/allowed-users"],
        PanelSession,
    );
    registry.register(
        [Method::POST],
        ["/api/security", "/api/security/"],
        PanelSession,
    );
    registry.register(
        [Method::GET, Method::POST],
        ["/api/mcp/token"],
        PanelSession,
    );

    // These two explicit not-found routes use `any`, so every standard
    // non-OPTIONS method is authenticated before the response is produced.
    registry.register(
        [
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::PATCH,
            Method::DELETE,
            Method::CONNECT,
            Method::TRACE,
        ],
        ["/api", "/file"],
        PanelOrMcp,
    );
    registry.register(
        [Method::GET],
        [
            "/file/{id}/{file_name}",
            "/api/banned-ips",
            "/api/banned-ips/",
            "/api/info",
            "/api/info/",
            "/api/logs/{file_name}/download",
            "/api/map/{save_name}",
            "/api/monitor",
            "/api/monitor/",
            "/api/monitor/history",
            "/api/monitor/activity",
            "/api/players/list",
            "/api/plugins/icon/{file_name}",
            "/api/version",
            "/api/version/",
            "/api/whitelist",
            "/api/whitelist/",
        ],
        PanelOrMcp,
    );
    registry.register(
        [Method::POST],
        [
            "/assets/upload/{name}",
            "/api/banned-ips/add",
            "/api/banned-ips/remove",
            "/api/control/stop",
            "/api/control/reload",
            "/api/control/restart",
            "/api/control/world",
            "/api/icon",
            "/api/icon/",
            "/api/info/motd",
            "/api/logs/{file_name}/upload-mclogs",
            "/api/map/{save_name}/tiles-range",
            "/api/map/{save_name}/tiles",
            "/api/players/op",
            "/api/players/deop",
            "/api/players/kick",
            "/api/players/ban",
            "/api/players/pardon",
            "/api/players/gamemode",
            "/api/whitelist/enable",
            "/api/whitelist/disable",
            "/api/whitelist/write",
            "/api/whitelist/add",
            "/api/whitelist/remove",
        ],
        PanelOrMcp,
    );
    registry.register([Method::DELETE], ["/assets/reset/{name}"], PanelOrMcp);
    registry.register(
        [Method::GET, Method::POST],
        [
            "/api/control/properties",
            "/api/control/paper-config",
            "/api/control/paper-world-config",
            "/api/control/launch-command",
            "/api/map",
            "/api/map/",
            "/api/saves",
            "/api/saves/",
            "/api/plugins",
            "/api/plugins/",
            "/api/terminal",
            "/api/terminal/",
            "/api/tasks",
            "/api/tasks/",
            "/api/mcp",
            "/api/mcp/",
            "/api/open-api",
            "/api/open-api/",
            "/api/open-api/{interface_name}",
        ],
        PanelOrMcp,
    );
    registry.register(
        [Method::GET, Method::POST, Method::DELETE],
        ["/api/control/code-of-conduct", "/api/plugins/{file_name}"],
        PanelOrMcp,
    );
    registry.register(
        [Method::GET, Method::POST, Method::PATCH],
        ["/api/gamerules/{dim_name}"],
        PanelOrMcp,
    );
    registry.register(
        [Method::GET, Method::POST, Method::PATCH, Method::DELETE],
        ["/api/saves/{save_name}"],
        PanelOrMcp,
    );
    registry.register(
        [Method::GET, Method::DELETE],
        [
            "/api/logs",
            "/api/logs/",
            "/api/logs/{file_name}",
            "/api/players",
            "/api/players/",
        ],
        PanelOrMcp,
    );
    registry.register(
        [Method::POST, Method::PATCH, Method::DELETE],
        ["/api/tasks/{id}"],
        PanelOrMcp,
    );

    registry
}

fn assets_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route(
            "/{name}",
            with_role(get(assets::get_asset), AuthRouteRole::Public),
        )
        .route(
            "/upload/{name}",
            with_role(post(assets::upload_asset), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/reset/{name}",
            with_role(
                axum::routing::delete(assets::reset_asset),
                AuthRouteRole::PanelOrMcp,
            ),
        )
}

fn file_router() -> Router<Arc<OPanel>> {
    with_role_router(
        Router::new()
            .route(
                "/{id}/{file_name}",
                with_method_fallback(get(download::download_file)),
            )
            .fallback(response::not_found),
        AuthRouteRole::PanelOrMcp,
    )
}

fn api_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route(
            "/auth",
            with_role(
                get(auth::get_cram).post(auth::validate_cram),
                AuthRouteRole::Public,
            ),
        )
        .route(
            "/auth/",
            with_role(
                get(auth::get_cram).post(auth::validate_cram),
                AuthRouteRole::Public,
            ),
        )
        .nest("/auth", {
            Router::new()
                .route(
                    "/check",
                    with_role(post(auth::check_auth), AuthRouteRole::Public),
                )
                .route(
                    "/logout",
                    with_role(post(auth::logout), AuthRouteRole::Public),
                )
                .nest("/oidc", {
                    Router::new()
                        .route("/login", with_role(get(oidc::login), AuthRouteRole::Public))
                        .route(
                            "/callback",
                            with_role(get(oidc::callback), AuthRouteRole::Public),
                        )
                        .route(
                            "/bind-user",
                            with_role(post(oidc::bind_new_user), AuthRouteRole::Public),
                        )
                        .route(
                            "/config",
                            with_role(get(oidc::get_config), AuthRouteRole::Public),
                        )
                        .route(
                            "/allowed-users",
                            with_role(
                                get(oidc::get_allowed_users)
                                    .post(oidc::add_allowed_user)
                                    .delete(oidc::remove_allowed_user),
                                AuthRouteRole::PanelSession,
                            ),
                        )
                        .fallback(response::not_found)
                })
                .fallback(response::not_found)
        })
        .route(
            "/banned-ips",
            with_role(get(banned_ips::get_banned_ips), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/banned-ips/",
            with_role(get(banned_ips::get_banned_ips), AuthRouteRole::PanelOrMcp),
        )
        .nest(
            "/banned-ips",
            with_role_router(
                Router::new()
                    .route("/add", with_method_fallback(post(banned_ips::ban_ip)))
                    .route("/remove", with_method_fallback(post(banned_ips::pardon_ip)))
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/control",
            with_role_router(
                Router::new()
                    .route(
                        "/properties",
                        with_method_fallback(
                            get(control::get_server_properties)
                                .post(control::set_server_properties),
                        ),
                    )
                    .route(
                        "/code-of-conduct",
                        with_method_fallback(
                            get(control::get_code_of_conducts)
                                .post(control::change_code_of_conduct)
                                .delete(control::remove_code_of_conduct),
                        ),
                    )
                    .route("/stop", with_method_fallback(post(control::stop_server)))
                    .route(
                        "/reload",
                        with_method_fallback(post(control::reload_server)),
                    )
                    .route(
                        "/restart",
                        with_method_fallback(post(control::restart_server)),
                    )
                    .route("/world", with_method_fallback(post(control::switch_save)))
                    .route(
                        "/paper-config",
                        with_method_fallback(
                            get(control::get_paper_server_config)
                                .post(control::set_paper_server_config),
                        ),
                    )
                    .route(
                        "/paper-world-config",
                        with_method_fallback(
                            get(control::get_paper_world_config)
                                .post(control::set_paper_world_config),
                        ),
                    )
                    .route(
                        "/launch-command",
                        with_method_fallback(
                            get(control::get_launch_command).post(control::set_launch_command),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/gamerules",
            with_role_router(
                Router::new()
                    .route(
                        "/{dim_name}",
                        with_method_fallback(
                            get(gamerules::get_gamerules)
                                .post(gamerules::change_gamerule)
                                .patch(gamerules::patch_gamerule),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/icon",
            with_method_fallback(
                role_layer(get(icon::get_favicon), AuthRouteRole::Public).merge(role_layer(
                    post(icon::upload_favicon),
                    AuthRouteRole::PanelOrMcp,
                )),
            ),
        )
        .route(
            "/icon/",
            with_method_fallback(
                role_layer(get(icon::get_favicon), AuthRouteRole::Public).merge(role_layer(
                    post(icon::upload_favicon),
                    AuthRouteRole::PanelOrMcp,
                )),
            ),
        )
        .route(
            "/info",
            with_role(get(info::get_server_info), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/info/",
            with_role(get(info::get_server_info), AuthRouteRole::PanelOrMcp),
        )
        .nest(
            "/info",
            with_role_router(
                Router::new()
                    .route("/motd", with_method_fallback(post(info::set_motd)))
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/logs",
            with_role(
                get(logs::get_log_file_list).delete(logs::clear_logs),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/logs/",
            with_role(
                get(logs::get_log_file_list).delete(logs::clear_logs),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/logs",
            with_role_router(
                Router::new()
                    .route(
                        "/{file_name}",
                        with_method_fallback(get(logs::get_log_content).delete(logs::delete_log)),
                    )
                    .route(
                        "/{file_name}/download",
                        with_method_fallback(get(logs::download_log)),
                    )
                    .route(
                        "/{file_name}/upload-mclogs",
                        with_method_fallback(post(logs::upload_log_to_mclogs)),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/map",
            with_role(
                get(map::get_map_enabled).post(map::toggle_map),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/map/",
            with_role(
                get(map::get_map_enabled).post(map::toggle_map),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/map",
            with_role_router(
                Router::new()
                    .route(
                        "/{save_name}",
                        with_method_fallback(get(map::get_available_tiles)),
                    )
                    .route(
                        "/{save_name}/tiles-range",
                        with_method_fallback(post(map::get_tiles_in_range)),
                    )
                    .route(
                        "/{save_name}/tiles",
                        with_method_fallback(post(map::get_tiles)),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/monitor",
            with_role(
                get(monitor::get_monitor_snapshot),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/monitor/",
            with_role(
                get(monitor::get_monitor_snapshot),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/monitor",
            with_role_router(
                Router::new()
                    .route("/history", with_method_fallback(get(monitor::get_history)))
                    .route(
                        "/activity",
                        with_method_fallback(get(monitor::get_activity)),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/players",
            with_role(
                get(players::get_players_overview).delete(players::delete_player_data),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/players/",
            with_role(
                get(players::get_players_overview).delete(players::delete_player_data),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/players",
            with_role_router(
                Router::new()
                    .route("/list", with_method_fallback(get(players::get_players)))
                    .route("/op", with_method_fallback(post(players::give_op)))
                    .route("/deop", with_method_fallback(post(players::deprive_op)))
                    .route("/kick", with_method_fallback(post(players::kick_player)))
                    .route("/ban", with_method_fallback(post(players::ban_player)))
                    .route(
                        "/pardon",
                        with_method_fallback(post(players::pardon_player)),
                    )
                    .route(
                        "/gamemode",
                        with_method_fallback(post(players::set_gamemode)),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/saves",
            with_role(
                get(saves::get_saves).post(saves::upload_save),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/saves/",
            with_role(
                get(saves::get_saves).post(saves::upload_save),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/saves",
            with_role_router(
                Router::new()
                    .route(
                        "/{save_name}",
                        with_method_fallback(
                            get(saves::download_save)
                                .post(saves::edit_save)
                                .patch(saves::toggle_save_datapack)
                                .delete(saves::delete_save),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/plugins",
            with_role(
                get(plugins::get_plugins).post(plugins::upload_plugin),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/plugins/",
            with_role(
                get(plugins::get_plugins).post(plugins::upload_plugin),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/plugins",
            with_role_router(
                Router::new()
                    .route(
                        "/icon/{file_name}",
                        with_method_fallback(get(plugins::get_plugin_icon)),
                    )
                    .route(
                        "/{file_name}",
                        with_method_fallback(
                            get(plugins::download_plugin)
                                .post(plugins::toggle_plugin)
                                .delete(plugins::delete_plugin),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/terminal",
            with_role(
                get(terminal::get_commands).post(terminal::send_command),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/terminal/",
            with_role(
                get(terminal::get_commands).post(terminal::send_command),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/security",
            with_role(
                post(security::update_access_key),
                AuthRouteRole::PanelSession,
            ),
        )
        .route(
            "/security/",
            with_role(
                post(security::update_access_key),
                AuthRouteRole::PanelSession,
            ),
        )
        .route(
            "/version",
            with_role(get(version::get_version_info), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/version/",
            with_role(get(version::get_version_info), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/whitelist",
            with_role(get(whitelist::get_whitelist), AuthRouteRole::PanelOrMcp),
        )
        .route(
            "/whitelist/",
            with_role(get(whitelist::get_whitelist), AuthRouteRole::PanelOrMcp),
        )
        .nest(
            "/whitelist",
            with_role_router(
                Router::new()
                    .route(
                        "/enable",
                        with_method_fallback(post(whitelist::enable_whitelist)),
                    )
                    .route(
                        "/disable",
                        with_method_fallback(post(whitelist::disable_whitelist)),
                    )
                    .route(
                        "/write",
                        with_method_fallback(post(whitelist::write_whitelist)),
                    )
                    .route(
                        "/add",
                        with_method_fallback(post(whitelist::add_whitelist_entry)),
                    )
                    .route(
                        "/remove",
                        with_method_fallback(post(whitelist::remove_whitelist_entry)),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/tasks",
            with_role(
                get(tasks::get_tasks).post(tasks::create_task),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/tasks/",
            with_role(
                get(tasks::get_tasks).post(tasks::create_task),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/tasks",
            with_role_router(
                Router::new()
                    .route(
                        "/{id}",
                        with_method_fallback(
                            post(tasks::edit_task)
                                .patch(tasks::toggle_task)
                                .delete(tasks::delete_task),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/mcp",
            with_role(
                get(mcp::get_mcp_enabled).post(mcp::toggle_mcp),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/mcp/",
            with_role(
                get(mcp::get_mcp_enabled).post(mcp::toggle_mcp),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/mcp",
            with_role_router(
                Router::new()
                    .route(
                        "/token",
                        with_method_fallback(
                            get(mcp::get_masked_access_token).post(mcp::generate_access_token),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelSession,
            ),
        )
        .route(
            "/open-api",
            with_role(
                get(open_api::get_open_api_enabled).post(open_api::toggle_open_api),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .route(
            "/open-api/",
            with_role(
                get(open_api::get_open_api_enabled).post(open_api::toggle_open_api),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .nest(
            "/open-api",
            with_role_router(
                Router::new()
                    .route(
                        "/{interface_name}",
                        with_method_fallback(
                            get(open_api::get_interface_enabled).post(open_api::toggle_interface),
                        ),
                    )
                    .fallback(response::not_found),
                AuthRouteRole::PanelOrMcp,
            ),
        )
        .fallback(response::not_found)
}

fn open_api_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route(
            "/info",
            with_method_fallback(get(openapi::info::get_server_info)),
        )
        .route(
            "/monitor",
            with_method_fallback(get(openapi::monitor::get_monitor)),
        )
        .route(
            "/plugins",
            with_method_fallback(get(openapi::plugins::get_plugins)),
        )
        .route(
            "/plugins/",
            with_method_fallback(get(openapi::plugins::get_plugins)),
        )
        .nest("/plugins", {
            Router::new()
                .route(
                    "/icon/{file_name}",
                    with_method_fallback(get(openapi::plugins::get_plugin_icon)),
                )
                .fallback(response::not_found)
        })
        .route(
            "/players",
            with_method_fallback(get(openapi::players::get_players)),
        )
        .route(
            "/players/",
            with_method_fallback(get(openapi::players::get_players)),
        )
        .nest("/players", {
            Router::new()
                .route(
                    "/{uuid}",
                    with_method_fallback(get(openapi::players::get_player_info)),
                )
                .fallback(response::not_found)
        })
        .route(
            "/logs",
            with_method_fallback(get(openapi::logs::get_log_file_list)),
        )
        .route(
            "/logs/",
            with_method_fallback(get(openapi::logs::get_log_file_list)),
        )
        .nest("/logs", {
            Router::new()
                .route(
                    "/{file_name}",
                    with_method_fallback(get(openapi::logs::get_log_content)),
                )
                .route(
                    "/{file_name}/download",
                    with_method_fallback(get(openapi::logs::download_log)),
                )
                .fallback(response::not_found)
        })
        .fallback(response::not_found)
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
