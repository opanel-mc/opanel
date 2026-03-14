# FRONTEND PANEL ROUTE GUIDE

## OVERVIEW
`frontend/app/panel` is the authenticated operations surface: feature routes for server management, refresh fan-out flows, and panel-shell composition.

## STRUCTURE
```text
panel/
|- layout.tsx                 # authenticated shell + sidebar/version wiring
|- page.tsx                   # panel root redirect behavior
|- sub-page.tsx               # shared route frame component
|- dashboard/ players/ saves/ plugins/ terminal/ tasks/  # high-traffic operations routes
|- code-of-conduct/ cloud-backup/ bukkit-config/         # config/editor style routes
`- settings/ mcp/ logs/ open-api/ gamerules/             # utility and integration routes
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Change panel shell behavior | `layout.tsx` + `sub-page.tsx` | Keep sidebar/version context centralized |
| Add feature route | `<feature>/page.tsx` | Prefer route-local state and transport calls via `frontend/lib` |
| Add modal flow | `<feature>/*-dialog.tsx` | Keep dialogs isolated from page files |
| Adjust shared data refresh behavior | `page.tsx` files using `emitter` | Existing fan-out event is `refresh-data` |
| Update table actions/columns | `<feature>/columns.tsx` | DataTable column defs stay in dedicated files |
| Add route tests | Existing `*.test.tsx` in feature folders | Current tests are concentrated in `players/` and `terminal/` |

## CONVENTIONS
- Keep backend transport centralized through `send*Request` helpers in `frontend/lib/api.ts`.
- Keep long-running feature-specific logic in feature-local helper files (`*-utils.ts`, feature components), not in shared shell files.
- When wiring refresh listeners, always clean up on unmount to avoid duplicate handlers.
- Follow existing control-flow style (`if(...) {`) and early-return guards for route checks.

## ANTI-PATTERNS
- Do not hardcode API hosts or duplicate request wrappers in panel routes.
- Do not place cross-feature logic in one feature folder when it belongs in `frontend/lib` or shared components.
- Do not edit generated web bundle files under `core/src/main/resources/web/_next`; edit sources in `frontend/` only.
- Do not bypass `panel/layout.tsx` composition by re-implementing sidebar/version context in individual pages.
