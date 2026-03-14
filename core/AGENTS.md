# CORE MODULE KNOWLEDGE BASE

## OVERVIEW
`core` is the platform-agnostic Java layer: HTTP API, websocket endpoints, auth/session logic, scheduling, storage contracts, and shared runtime orchestration.

## STRUCTURE
```text
core/
|- src/main/java/net/opanel/              # core runtime packages
|- src/main/resources/web/                # bundled frontend assets served by Javalin
`- build.gradle                           # core dependencies and generated opanel.properties
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Register/adjust HTTP route | `src/main/java/net/opanel/web/WebServer.java` | Route definitions + middleware wiring |
| Add internal panel API | `src/main/java/net/opanel/controller/api` | Uses `BaseController` response helpers |
| Add Open API route | `src/main/java/net/opanel/controller/openapi` | Exposed under `/open-api/*` |
| Change auth/jwt/cookies | `src/main/java/net/opanel/controller/BeforeController.java` + `src/main/java/net/opanel/web` | Request gating lives in before handlers |
| Change websocket payload flow | `src/main/java/net/opanel/endpoint` | `/socket/players`, `/socket/inventory/{uuid}`, `/socket/terminal` |
| Change startup lifecycle | `src/main/java/net/opanel/OPanel.java` | Owns config, task manager, web server init |

## CONVENTIONS
- Keep `core` free of loader/version APIs (`org.bukkit.*`, `net.fabricmc.*`, `net.minecraftforge.*`).
- Add new API endpoints as controller handlers first, then wire routes in `WebServer`.
- Keep response shape consistent through `BaseController` helpers.
- Use early returns for guards and keep control flow style `if(...) {`.
- When adding shaded dependencies in version modules, relocation target must remain `net.opanel.deps.*`.

## ANTI-PATTERNS (CORE)
- Do not add Minecraft-loader-specific code to `core`; place it in helper/version modules.
- Do not edit generated frontend files under `src/main/resources/web/_next`.
- Do not bypass route middleware when adding endpoints (auth/open-api gating must remain explicit).

## COMMANDS
```bash
./gradlew :core:build
./gradlew :core:tasks
```

## NOTES
- `build.gradle` task `generateOPanelProperties` writes version metadata consumed by `OPanel.VERSION`.
- Frontend bundle in `src/main/resources/web` is generated from `frontend/scripts/bundle.js`.
