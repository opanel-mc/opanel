# FRONTEND APP ROUTING GUIDE

## OVERVIEW
`frontend/app` contains route-level composition: authentication gate, panel shell, and feature pages for server operations.

## STRUCTURE
```text
app/
|- layout.tsx                 # root providers/theme/toast/bootstrap
|- page.tsx                   # root redirect/auth check
|- login/                     # login flow UI
|- panel/                     # authenticated management pages
`- about/                     # project/about pages
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Change global shell/providers | `layout.tsx` | Theme + app init |
| Change auth entry flow | `login/page.tsx` + `page.tsx` | Uses `useCheckAuth` and challenge flow |
| Change authenticated shell | `panel/layout.tsx` | Sidebar + version context |
| Add panel feature page | `panel/<feature>/page.tsx` | Keep feature state local to route |
| Change panel-specific feature conventions | `panel/AGENTS.md` | Feature boundaries, refresh/event patterns, route shell contracts |
| Add page-specific modal | `panel/**/**-dialog.tsx` | Dialog split is required convention |

## CONVENTIONS
- Page components should keep transport logic concise and delegate reusable logic to `frontend/lib` or feature helpers.
- Use `emitter`-based `refresh-data` fan-out where pages need coordinated reloads.
- Keep control-flow style aligned with repo (`if(...) {`, early returns for guard checks).

## ANTI-PATTERNS
- Do not put shared transport/util functions directly in route pages; move to `frontend/lib`.
- Do not duplicate cross-route layout concerns (sidebar/version context) inside individual `panel/<feature>/page.tsx` files.
- Do not run parallel fetch loops across sibling pages when a route-level refresh signal (`refresh-data`) already coordinates updates.
