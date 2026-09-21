use std::sync::Arc;

use axum::{
    Router,
    routing::{MethodRouter, any, get, post},
};

use crate::opanel::OPanel;

use super::response;

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
        .nest("/assets", assets_router())
        .nest("/file", file_router())
        .nest("/api", api_router())
        .nest("/open-api", open_api_router())
        .route("/api", any(response::not_found))
        .route("/file", any(response::not_found))
        .route("/open-api", any(response::not_found))
}

fn with_method_fallback(router: MethodRouter<Arc<OPanel>>) -> MethodRouter<Arc<OPanel>> {
    router.fallback(response::method_not_allowed)
}

fn assets_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route("/{name}", with_method_fallback(get(assets::get_asset)))
        .route(
            "/upload/{name}",
            with_method_fallback(post(assets::upload_asset)),
        )
        .route(
            "/reset/{name}",
            with_method_fallback(axum::routing::delete(assets::reset_asset)),
        )
}

fn file_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route(
            "/{id}/{file_name}",
            with_method_fallback(get(download::download_file)),
        )
        .fallback(response::not_found)
}

fn api_router() -> Router<Arc<OPanel>> {
    Router::new()
        .route(
            "/auth",
            with_method_fallback(get(auth::get_cram).post(auth::validate_cram)),
        )
        .route(
            "/auth/",
            with_method_fallback(get(auth::get_cram).post(auth::validate_cram)),
        )
        .nest("/auth", {
            Router::new()
                .route("/check", with_method_fallback(post(auth::check_auth)))
                .route("/logout", with_method_fallback(post(auth::logout)))
                .nest("/oidc", {
                    Router::new()
                        .route("/login", with_method_fallback(get(oidc::login)))
                        .route("/callback", with_method_fallback(get(oidc::callback)))
                        .route(
                            "/bind-user",
                            with_method_fallback(post(oidc::bind_new_user)),
                        )
                        .route("/config", with_method_fallback(get(oidc::get_config)))
                        .route(
                            "/allowed-users",
                            with_method_fallback(
                                get(oidc::get_allowed_users)
                                    .post(oidc::add_allowed_user)
                                    .delete(oidc::remove_allowed_user),
                            ),
                        )
                        .fallback(response::not_found)
                })
                .fallback(response::not_found)
        })
        .route(
            "/banned-ips",
            with_method_fallback(get(banned_ips::get_banned_ips)),
        )
        .route(
            "/banned-ips/",
            with_method_fallback(get(banned_ips::get_banned_ips)),
        )
        .nest("/banned-ips", {
            Router::new()
                .route("/add", with_method_fallback(post(banned_ips::ban_ip)))
                .route("/remove", with_method_fallback(post(banned_ips::pardon_ip)))
                .fallback(response::not_found)
        })
        .nest("/control", {
            Router::new()
                .route(
                    "/properties",
                    with_method_fallback(
                        get(control::get_server_properties).post(control::set_server_properties),
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
                        get(control::get_paper_world_config).post(control::set_paper_world_config),
                    ),
                )
                .route(
                    "/launch-command",
                    with_method_fallback(
                        get(control::get_launch_command).post(control::set_launch_command),
                    ),
                )
                .fallback(response::not_found)
        })
        .nest("/gamerules", {
            Router::new()
                .route(
                    "/{dim_name}",
                    with_method_fallback(
                        get(gamerules::get_gamerules)
                            .post(gamerules::change_gamerule)
                            .patch(gamerules::patch_gamerule),
                    ),
                )
                .fallback(response::not_found)
        })
        .route(
            "/icon",
            with_method_fallback(get(icon::get_favicon).post(icon::upload_favicon)),
        )
        .route(
            "/icon/",
            with_method_fallback(get(icon::get_favicon).post(icon::upload_favicon)),
        )
        .route("/info", with_method_fallback(get(info::get_server_info)))
        .route("/info/", with_method_fallback(get(info::get_server_info)))
        .nest("/info", {
            Router::new()
                .route("/motd", with_method_fallback(post(info::set_motd)))
                .fallback(response::not_found)
        })
        .route(
            "/logs",
            with_method_fallback(get(logs::get_log_file_list).delete(logs::clear_logs)),
        )
        .route(
            "/logs/",
            with_method_fallback(get(logs::get_log_file_list).delete(logs::clear_logs)),
        )
        .nest("/logs", {
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
                .fallback(response::not_found)
        })
        .route(
            "/map",
            with_method_fallback(get(map::get_map_enabled).post(map::toggle_map)),
        )
        .route(
            "/map/",
            with_method_fallback(get(map::get_map_enabled).post(map::toggle_map)),
        )
        .nest("/map", {
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
                .fallback(response::not_found)
        })
        .route(
            "/monitor",
            with_method_fallback(get(monitor::get_monitor_snapshot)),
        )
        .route(
            "/monitor/",
            with_method_fallback(get(monitor::get_monitor_snapshot)),
        )
        .nest("/monitor", {
            Router::new()
                .route("/history", with_method_fallback(get(monitor::get_history)))
                .route(
                    "/activity",
                    with_method_fallback(get(monitor::get_activity)),
                )
                .fallback(response::not_found)
        })
        .route(
            "/players",
            with_method_fallback(
                get(players::get_players_overview).delete(players::delete_player_data),
            ),
        )
        .route(
            "/players/",
            with_method_fallback(
                get(players::get_players_overview).delete(players::delete_player_data),
            ),
        )
        .nest("/players", {
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
                .fallback(response::not_found)
        })
        .route(
            "/saves",
            with_method_fallback(get(saves::get_saves).post(saves::upload_save)),
        )
        .route(
            "/saves/",
            with_method_fallback(get(saves::get_saves).post(saves::upload_save)),
        )
        .nest("/saves", {
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
                .fallback(response::not_found)
        })
        .route(
            "/plugins",
            with_method_fallback(get(plugins::get_plugins).post(plugins::upload_plugin)),
        )
        .route(
            "/plugins/",
            with_method_fallback(get(plugins::get_plugins).post(plugins::upload_plugin)),
        )
        .nest("/plugins", {
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
                .fallback(response::not_found)
        })
        .route(
            "/terminal",
            with_method_fallback(get(terminal::get_commands).post(terminal::send_command)),
        )
        .route(
            "/terminal/",
            with_method_fallback(get(terminal::get_commands).post(terminal::send_command)),
        )
        .route(
            "/security",
            with_method_fallback(post(security::update_access_key)),
        )
        .route(
            "/security/",
            with_method_fallback(post(security::update_access_key)),
        )
        .route(
            "/version",
            with_method_fallback(get(version::get_version_info)),
        )
        .route(
            "/version/",
            with_method_fallback(get(version::get_version_info)),
        )
        .route(
            "/whitelist",
            with_method_fallback(get(whitelist::get_whitelist)),
        )
        .route(
            "/whitelist/",
            with_method_fallback(get(whitelist::get_whitelist)),
        )
        .nest("/whitelist", {
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
                .fallback(response::not_found)
        })
        .route(
            "/tasks",
            with_method_fallback(get(tasks::get_tasks).post(tasks::create_task)),
        )
        .route(
            "/tasks/",
            with_method_fallback(get(tasks::get_tasks).post(tasks::create_task)),
        )
        .nest("/tasks", {
            Router::new()
                .route(
                    "/{id}",
                    with_method_fallback(
                        post(tasks::edit_task)
                            .patch(tasks::toggle_task)
                            .delete(tasks::delete_task),
                    ),
                )
                .fallback(response::not_found)
        })
        .route(
            "/mcp",
            with_method_fallback(get(mcp::get_mcp_enabled).post(mcp::toggle_mcp)),
        )
        .route(
            "/mcp/",
            with_method_fallback(get(mcp::get_mcp_enabled).post(mcp::toggle_mcp)),
        )
        .nest("/mcp", {
            Router::new()
                .route(
                    "/token",
                    with_method_fallback(
                        get(mcp::get_masked_access_token).post(mcp::generate_access_token),
                    ),
                )
                .fallback(response::not_found)
        })
        .route(
            "/open-api",
            with_method_fallback(
                get(open_api::get_open_api_enabled).post(open_api::toggle_open_api),
            ),
        )
        .route(
            "/open-api/",
            with_method_fallback(
                get(open_api::get_open_api_enabled).post(open_api::toggle_open_api),
            ),
        )
        .nest("/open-api", {
            Router::new()
                .route(
                    "/{interface_name}",
                    with_method_fallback(
                        get(open_api::get_interface_enabled).post(open_api::toggle_interface),
                    ),
                )
                .fallback(response::not_found)
        })
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
