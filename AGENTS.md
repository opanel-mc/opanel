# Repository Guidelines

## Project Structure & Module Organization
`settings.gradle` defines a multi-module Gradle build. Shared services live in `core/`, while the `spigot-*`, `folia-*`, `fabric-*`, `forge-*`, and `neoforge-*` modules only contain platform bootstraps that delegate to `core/src/main/java`. The Next.js dashboard sits in `frontend/` (`app/` routes, `components/` UI atoms, `contexts/` for state). Each module writes to `<module>/build/`; keep edits inside `src/main/java` or `src/main/resources` so Gradle detects changes cleanly.

## Build, Test, and Development Commands
- `./gradlew build` compiles every adapter, runs resource generation (including `opanel.properties`), and writes jars to `<module>/build/libs`. Use `./gradlew :spigot-1.21:build` (swap module as needed) for quicker iteration.
- `./gradlew test` runs JVM unit tests across all modules; run it locally before every PR.
- `cd frontend && npm install`, `npm run dev` for `http://localhost:3001`, `npm run build` to ship the optimized bundle consumed by `scripts/copy`, and `npm run lint` to enforce the shared ESLint + TypeScript rules.

## Coding Style & Naming Conventions
Java sources target language level 14 (see `core/build.gradle`) and use four-space indentation plus `UpperCamelCase` types and `lowerCamelCase` members. Keep packages under `org.opanel.*` and avoid platform-specific logic in `core`. Frontend code is TypeScript-first: keep React components functional, organize primitives under `components/ui-*`, prefer kebab-case route folders in `app/`, and camelCase hooks. `npm run lint` must pass before committing.

## Testing Guidelines
Add JVM tests under `<module>/src/test/java` using JUnit 5 via the default Gradle `test` task; focus on serialization, scheduler safety, and platform boundaries. Frontend automation is still being introduced, so pair UI changes with lightweight React Testing Library coverage when practical or, at minimum, manual verification steps plus screenshots. Changes that touch authentication, file transfers, or command dispatch must include a regression test in `core`.

## Commit & Pull Request Guidelines
Git history follows conventional commits (`fix(monitor): …`, `chore: …`). Keep scopes aligned with modules (`frontend`, `core`, `spigot`, etc.) or subsystems (`monitor`, `auth`). PRs should describe motivation, list validation commands (`./gradlew build`, `npm run lint`, screenshots), reference related issues, and highlight config changes (e.g., new keys in generated properties). Small, rebased commits review faster than omnibus drops.

## Security & Configuration Tips
Do not commit secrets, server addresses, or personal data—inject them at runtime via environment variables or your hosting panel, and never edit `opanel.properties` by hand because Gradle regenerates it. Scrub tokens and IPs from logs or screenshots before sharing, and rely on Maven/npm dependencies instead of vendored binaries so the supply chain stays auditable.
