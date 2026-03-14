# PROJECT KNOWLEDGE BASE

**Generated:** 2026-03-12 13:21 CST  
**Commit:** c7071c37  
**Branch:** main

## OVERVIEW
OPanel is a multi-module Minecraft server panel project: `core` holds platform-agnostic logic, `*-helper` modules hold loader-shared adapters, and versioned modules (`spigot-*`, `fabric-*`, `forge-*`, `folia-*`, `neoforge-*`) wire runtime entry points.
Frontend is Next.js and is bundled into `core/src/main/resources/web` for release artifacts.

## STRUCTURE
```text
opanel/
|- core/                               # Java business core (Javalin, auth, tasks, storage)
|- frontend/                           # Next.js source (runs on localhost:3001 in dev)
|- bukkit-helper/ fabric-helper/ forge-helper/
|- spigot-* / folia-*                  # Bukkit-family adapters by MC version
|- fabric-*                            # Fabric adapters by MC version
|- forge-* / neoforge-*                # Forge-family adapters by MC version
|- .github/workflows/                  # CI build/publish pipelines
|- build/libs/                         # assembled jars from module builds
`- core/src/main/resources/web/        # generated frontend bundle copied by frontend/scripts/bundle.js
```

## AGENTS HIERARCHY
| Scope | Path | Use For |
|------|------|---------|
| Root | `AGENTS.md` | Global architecture, cross-module rules, top-level commands |
| Core backend | `core/AGENTS.md` + `core/src/main/java/net/opanel/AGENTS.md` | API routes, middleware, websocket/runtime wiring |
| Frontend | `frontend/AGENTS.md` + `frontend/app/AGENTS.md` + `frontend/app/panel/AGENTS.md` + `frontend/components/AGENTS.md` + `frontend/lib/AGENTS.md` | Next.js routing, panel-route contracts, shared UI, request/util layers |
| Loader helpers | `bukkit-helper/AGENTS.md`, `fabric-helper/AGENTS.md`, `forge-helper/AGENTS.md` | Loader-shared adapter logic |
| Version adapters | `spigot-1.21/AGENTS.md`, `folia-1.21/AGENTS.md`, `fabric-1.21/AGENTS.md`, `forge-1.21/AGENTS.md`, `neoforge-1.21.1/AGENTS.md` | Version-specific `Main.java` bootstrap and metadata wiring |

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add API endpoint | `core/src/main/java/net/opanel/controller/api` | Register route in `core/src/main/java/net/opanel/web/WebServer.java` |
| Add Open API endpoint | `core/src/main/java/net/opanel/controller/openapi` | Guarded by Open API toggle middleware |
| Update websocket behavior | `core/src/main/java/net/opanel/endpoint` | `/socket/*` endpoints registered in `WebServer` |
| Change auth/session flow | `core/src/main/java/net/opanel/web` + `core/src/main/java/net/opanel/controller/BeforeController.java` | JWT + before filters live here |
| Add frontend page | `frontend/app` | Route-level pages/layouts (App Router) |
| Reusable UI component | `frontend/components` | Shared components + shadcn wrappers |
| Frontend data/API utility | `frontend/lib` | Central API wrappers and protocol helpers |
| Frontend test helpers | `frontend/test` | Includes i18n/font mocks and utility wrappers |
| Platform shared code | `bukkit-helper` / `fabric-helper` / `forge-helper` | Put loader-shared code here before version module |
| Platform/version bootstrap | `<loader>-<version>/src/main/java/**/Main.java` | Entry point starts web server and binds platform server |

## CODE MAP
LSP servers are unavailable in this environment (`typescript-language-server`, `jdtls` missing), so this map is file-backed instead of symbol-index-backed.

| Symbol/Area | Location | Role |
|-------------|----------|------|
| `OPanel` | `core/src/main/java/net/opanel/OPanel.java` | Runtime orchestrator, config lifecycle, task manager, web server holder |
| `WebServer` | `core/src/main/java/net/opanel/web/WebServer.java` | HTTP routes, WS endpoints, static web assets, error handling |
| `InfoController` | `core/src/main/java/net/opanel/controller/api/InfoController.java` | Example API controller pattern |
| `PanelLayout` | `frontend/app/panel/layout.tsx` | Auth-gated shell and sidebar composition |
| `send*Request` | `frontend/lib/api.ts` | Centralized HTTP wrappers and error toast flow |
| `emitter` | `frontend/lib/emitter.ts` | Shared EventEmitter singleton used for `refresh-data` refresh fan-out |

## CONVENTIONS
- Naming: frontend files/folders use kebab-case; Java package/version segments use snake_case; Java classes use PascalCase.
- Control flow style: no space after `if/for/while/switch`; keep braces as `if(...) {` (not newline brace style).
- Prefer early-return short guards (`if(!x) return;`) for simple preconditions.
- Frontend import ordering follows `frontend/eslint.config.mjs` (`type`/`builtin`/`external` grouping).
- Dialog components must be isolated into `*-dialog.tsx` files.
- DataTable column definitions must be isolated into `columns.tsx` files.
- Shadow relocation target must stay under `net.opanel.deps.*`.

## ANTI-PATTERNS (THIS PROJECT)
- Do not edit generated bundle files inside `core/src/main/resources/web/_next`.
- Do not run `npm run build` in routine frontend dev flows; `frontend/scripts/bundle.js` replaces `core/src/main/resources/web` and causes churn.
- Do not put platform/version-specific server API code in `core`; use helpers/version modules.
- Do not duplicate loader-shared logic across many version modules; move shared parts into the corresponding `*-helper` module.

## UNIQUE STYLES
- Runtime shape is "core interfaces + helper shared implementation + version adapter"; preserve this layering.
- Every platform `Main.java` starts OPanel web server (default comment `port 3000`) after platform server is ready.
- Frontend dev server runs on `3001`, backend panel API/websocket dev target remains `3000`.

## COMMANDS
```bash
./gradlew build
./gradlew :core:build
./gradlew :spigot-1.21:build

cd frontend && npm run dev
cd frontend && npm run lint
cd frontend && npm run test
```

## NOTES
- CI (`.github/workflows/build.yml`) runs frontend lint/tests/build plus Gradle build.
- Publish pipeline (`.github/workflows/publish.yml`) distributes generated jars to platform channels.
- Frontend uses Vitest (`frontend/test` plus `**/tests`); Java modules currently have no dedicated test source sets.
- For local frontend testing with real backend, start an OPanel-enabled server with web panel port at `3000`.
