# FRONTEND COMPONENTS GUIDE

## OVERVIEW
`frontend/components` hosts reusable UI building blocks and cross-page widgets used by route pages in `frontend/app`.

## STRUCTURE
```text
components/
|- ui/                        # shadcn-style primitives and wrappers
|- tests/                     # component-level vitest cases
|- data-table.tsx             # shared table with optional URL pagination state
|- app-sidebar.tsx            # panel navigation shell
`- *.tsx                      # reusable view widgets (terminal, prompts, inputs)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Modify base visual primitives | `ui/*.tsx` | Keep primitive API stable for route pages |
| Add DataTable behavior | `data-table.tsx` | Shared pagination/query behavior used by multiple pages |
| Change sidebar nav rules | `app-sidebar.tsx` | Version-aware visibility and active-route logic |
| Update component tests | `tests/*.test.tsx` | Vitest + Testing Library patterns |

## CONVENTIONS
- Keep reusable components prop-driven; avoid embedding page-specific fetch/state side effects.
- Use existing i18n helpers (`$`) and utility class merge helper (`cn`) instead of duplicating formatting logic.
- For shared widgets with internal state (table/sidebar/viewers), keep state transitions deterministic and externally controllable via props where possible.

## ANTI-PATTERNS
- Do not duplicate shared primitives already present in `ui/`.
- Do not move route-scoped feature logic into generic components unless it is reused by multiple routes.
- Do not hardcode route paths or backend endpoints in generic components; pass behavior from route/container layers.
