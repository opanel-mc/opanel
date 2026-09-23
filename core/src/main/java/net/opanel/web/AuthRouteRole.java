package net.opanel.web;

import io.javalin.security.RouteRole;

public enum AuthRouteRole implements RouteRole {
    PUBLIC,
    PANEL_SESSION,
    PANEL_OR_MCP
}
