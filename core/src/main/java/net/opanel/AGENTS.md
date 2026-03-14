# NET.OPANEL PACKAGE GUIDE

## OVERVIEW
This package tree contains the main runtime domains for OPanel core: controllers, endpoints, scheduling, storage, terminal, and web server composition.

## STRUCTURE
```text
net/opanel/
|- OPanel.java               # runtime root object
|- controller/               # API/OpenAPI handlers and request middleware
|- web/                      # Javalin boot, route graph, auth/cors handling
|- endpoint/                 # websocket endpoints
|- common/ config/ logger/   # shared contracts and runtime services
|- storage/ task/ terminal/  # persistence, cron tasks, log streaming
`- utils/ time/ event/       # utility, timing, event integrations
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add route path | `web/WebServer.java` | Central route graph; keep endpoint grouping |
| Implement API business handler | `controller/api/*.java` | Follow `InfoController` style with explicit status handling |
| Add Open API feature | `controller/openapi/*.java` | Must stay behind Open API toggle middleware |
| Add websocket channel | `endpoint/*.java` + `web/WebServer.java` | Register via `app.ws(...)` |
| Modify startup/shutdown | `OPanel.java` | Owns init, stop lifecycle, task manager wiring |
| Change periodic server updates | `OPanel.java` + `time/TPS.java` | `onTick()` is called by loader modules |

## CONVENTIONS
- Keep package boundaries clear: web wiring in `web`, request handlers in `controller`, realtime channels in `endpoint`.
- Keep `controller/api` and `controller/openapi` split by exposure level; avoid mixing internal/admin handlers into open-api package.
- Keep data conversion/encoding logic in `utils` or dedicated domain helpers, not duplicated across controllers.

## ANTI-PATTERNS
- Do not register new routes outside `web/WebServer.java`.
- Do not place cross-cutting middleware logic directly inside each controller handler.
- Do not bypass `BeforeController` gates when adding new API groups.
