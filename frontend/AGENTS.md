# FRONTEND MODULE KNOWLEDGE BASE

## OVERVIEW
`frontend` is a Next.js App Router project (React + TypeScript + Tailwind + shadcn) that targets backend APIs on `localhost:3000` during development.

## STRUCTURE
```text
frontend/
|- app/                    # routes, layouts, feature pages
|- components/             # shared UI and composable widgets
|- contexts/ hooks/        # app-wide state and behavior hooks
|- lib/                    # API wrappers, protocol/data utilities
|- test/                   # reusable test setup/mocks/helpers
|- lang/                   # i18n dictionaries
`- scripts/                # prelaunch/bundle scripts
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add or change route UI | `app/**` | App Router pages/layouts |
| Add reusable component | `components/**` | Keep generic UI isolated from route state |
| Add API or request helper | `lib/api.ts` + `lib/**` | Centralize transport/error behavior |
| Add shared refresh behavior | `lib/emitter.ts` + page hooks | Existing fan-out event is `refresh-data` |
| Add test utility/mocks | `test/**` | Includes i18n and next/font mocking |

## CONVENTIONS
- Follow import ordering from `eslint.config.mjs` (`type` -> `builtin` -> `external`).
- Route files use kebab-case naming; keep page-specific dialogs in `*-dialog.tsx`.
- DataTable column definitions belong in dedicated `columns.tsx` files.
- Reuse `send*Request` wrappers from `lib/api.ts` instead of ad hoc axios usage.

## ANTI-PATTERNS (FRONTEND)
- Do not run `npm run build` for routine local edits; it triggers `scripts/bundle.js` and rewrites `../core/src/main/resources/web`.
- Do not bypass centralized request wrappers for API error handling.
- Do not spread duplicated i18n/test mock logic across test files when helpers exist in `test/`.

## COMMANDS
```bash
npm run dev
npm run lint
npm run test
npm run test:watch
npm run prelaunch
```

## NOTES
- Dev server runs on `3001`; backend API/ws target remains `3000` in development.
- CI does run frontend build in isolation; local warning is about repository churn, not build validity.
