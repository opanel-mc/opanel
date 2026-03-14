# FRONTEND LIB GUIDE

## OVERVIEW
`frontend/lib` is the shared logic layer for transport, protocol parsing, i18n/settings access, and feature-domain utilities.

## STRUCTURE
```text
lib/
|- api.ts                     # HTTP + upload wrappers and error handling
|- emitter.ts                 # shared EventEmitter singleton
|- i18n.ts / settings.ts      # localization and persisted settings helpers
|- ws/                        # websocket client-side protocol helpers
|- nbt/ formatting-codes/     # minecraft text/data parsing helpers
`- server-config/ gamerules/  # server properties and gamerule utility domains
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| New API transport behavior | `api.ts` | Keep status handling and redirect behavior centralized |
| Shared refresh event behavior | `emitter.ts` | Existing usage relies on `refresh-data` |
| i18n utility changes | `i18n.ts` + `lang/*.json` | Test mocks depend on this interface |
| Websocket helper updates | `ws/**` | Keep protocol logic out of route files |
| Domain parsing utilities | `nbt/**`, `formatting-codes/**`, `server-config/**` | Prefer pure/stateless helpers |

## CONVENTIONS
- Preserve API wrapper pattern (`sendGetRequest`, `sendPostRequest`, etc.) and shared error handling through `toastError`.
- Keep helpers framework-agnostic where possible so route pages remain thin.
- Keep type contracts in `types.ts` or close-to-domain helper modules, not duplicated in pages.

## ANTI-PATTERNS
- Do not bypass `api.ts` with direct axios usage in route components.
- Do not expand EventEmitter usage with ad hoc event names unless updating all consumers intentionally.
- Do not duplicate parser/formatter code in UI components when a `lib` domain module already exists.
