# Building OPanel

Use Node.js 22 with npm and the Java toolchains required by the selected module
(CI installs JDK 25, 21, 17 and 14, and starts Gradle with JDK 25). A local build
also needs Rust/Cargo for the frontend Wasm preparation; `wasm-pack` is installed
with the frontend npm dependencies.

## Java platforms

Install frontend dependencies before the first build and after dependency
changes, then invoke Gradle. Run from the repository root:

```sh
npm --prefix frontend ci
./gradlew -PbuildTarget=paper-26.1 :paper-26.1:build --stacktrace
```

Windows PowerShell:

```powershell
npm.cmd --prefix frontend ci
.\gradlew.bat -PbuildTarget=paper-26.1 :paper-26.1:build --stacktrace
```

Subsequent builds can invoke Gradle directly while the installed dependencies
remain current. Dependency installation is managed externally; Gradle does not
install or update npm packages.

`buildTarget` limits the configured projects to this target and its dependencies.
The task path selects what to build. Targets and their helpers are registered in
`platform-modules.json`; Gradle project names remain flat even though
their directories are grouped by platform. Omit `buildTarget` when requesting
several modules, or use `./gradlew build` for all modules. API-only publishing
continues to use `-PapiOnly=true`.

Gradle checks that the required frontend tools are installed, prepares shared
Minecraft assets and Wasm once, and invokes `npm run build` for each requested
target. Missing tools fail the build with an installation hint. You can also
invoke `:<target>:buildFrontend` to build only that target's frontend.

Each version module declares frontend configuration in its `gradle.properties`:

```properties
frontend_env_vite_opanel_target=paper-26.1
```

Gradle strips `frontend_env_`, uppercases the remaining name, and passes the value
unchanged to the npm process. Add further prefixed properties to inject more
variables. Values come from these properties, not from inferred module names.
The resulting variables are tracked as frontend task inputs. Frontend sources,
configuration, lockfiles and prepared resources are also tracked, allowing a
repeat build or a Java-only edit to reuse the frontend output.

Gradle controls two internal variables automatically:

- `OPANEL_FRONTEND_OUTPUT`: the absolute path to this module's `build/frontend`.
- `OPANEL_FRONTEND_PREPARED=1`: validate already prepared resources and skip their
  generation. This does not skip frontend compilation.

The current vinext static exporter uses `frontend/dist` internally. The build
script finishes compilation and generates the compatibility ID there, then
publishes the complete output to the module directory. A shared Gradle service
serializes frontend tasks within one Gradle invocation, including publication;
Java compilation can still run in parallel. Do not run separate frontend builds
simultaneously in the same checkout.

Each module packages `client/` as `opanel-web/` and places the matching
`vinext-rsc-compatibility-id` at the JAR resource root. Final JARs remain under
the root `build/libs`, for example `opanel-paper-26.1-build-2.2.4.jar`.
Inventory textures remain fully bundled; map assets and Minecraft translations
retain their shared generation strategy.

## Pumpkin

Pumpkin does not use Gradle. Build its frontend before invoking Cargo:

```sh
npm --prefix frontend ci
VITE_OPANEL_TARGET=pumpkin-26.3 npm --prefix frontend run build
cargo build --release --locked
```

Windows PowerShell:

```powershell
npm.cmd --prefix frontend ci
$env:VITE_OPANEL_TARGET = "pumpkin-26.3"
npm.cmd --prefix frontend run build
cargo build --release --locked
```

Leave `OPANEL_FRONTEND_OUTPUT` unset for Pumpkin so the output stays in
`frontend/dist`, where its Rust asset crate embeds it.

## Development and verification

`npm --prefix frontend run dev` defaults the target to `development`.
Production builds require a nonempty `VITE_OPANEL_TARGET`. Its value is compiled
into the frontend; changing server runtime environment variables does not change
an existing build. After opening the panel, inspect:

```js
window.__OPANEL_BUILD_INFO__.target
```

Local checks without a full frontend or Gradle build:

```sh
npm --prefix frontend run lint
npm --prefix frontend run typecheck
node --test .github/scripts/build-matrix.test.mjs
```

## CI and generated files

The `CI` workflow (`.github/workflows/ci.yml`) starts a lightweight `changes` job
on every push to `main` and pull request. It
compares commits and writes the selected targets and skip reasons to the run
summary. Path rules live in `.github/scripts/detect-builds.mjs`, rather than
duplicating them in workflow-level `paths-ignore` filters.

| Change | Java targets | Pumpkin |
| --- | --- | --- |
| One version module | That target only | Skip |
| Helper/config module | Its consumers from the module registry | Skip |
| Core, API or shared Gradle configuration | All targets | Skip |
| Frontend code, assets, dependencies or build scripts | All targets | Build |
| Pumpkin code or root Cargo manifest/lockfile | Skip | Build |
| Build workflow or change detection | All targets | Build |
| Anything under `example-extension/` | Skip | Skip |
| Documentation, display images, editor settings, `.gitignore`, known publishing files | Skip | Skip |

Changed paths are combined and targets deduplicated. Unknown Java platform paths
select all Java targets; other unknown inputs conservatively select all products.
The example extension is not built by this workflow. Changes under
`example-extension/` are ignored; other changed paths still select builds normally.

Push detection compares the entire pushed range. PR detection compares the PR
head against its merge base with the base branch, including earlier PR commits
but excluding unrelated base-branch changes. Deleted files and both paths of a
rename are included. Unavailable comparison history falls back to all products
and frontend checks; invalid module configuration fails detection instead.

The shared `prepare` job runs only when a Java target or Pumpkin needs a frontend.
It generates and uploads Minecraft assets and Wasm outputs. The separate
`frontend-check` job downloads those inputs and runs frontend lint, type checking,
unit tests and Wasm tests. It runs for frontend changes, frontend Gradle task
changes, changes to the build workflow/detection, full-build fallbacks and manual
product builds.

Backend-only changes still need resource preparation and target frontend
compilation in a fresh checkout, but skip `frontend-check`. Java and Pumpkin
builds wait for preparation to succeed and frontend checks to pass or be skipped
by the change rules. Failed or cancelled frontend checks block those builds.

The Java matrix is generated from the module registry (currently 39 targets)
and filtered to the selected targets. Each job installs npm dependencies in a
separate workflow step before invoking Gradle, downloads the shared inputs,
sets `OPANEL_FRONTEND_PREPARED=1`, and builds and uploads its own JAR. Automatic
runs only upload affected targets; they do not mix in artifacts from older runs.
Use manual `build_jar` / `build_pumpkin` inputs to force complete builds of the
enabled branches. Selecting neither skips preparation, checks and builds after
detection. Matrix failures do not cancel other targets; GitHub's available
concurrency determines scheduling.

Pumpkin builds its frontend once in a separate job and reuses it for Rust checks
and all five native targets. Intermediate artifacts are retained for seven days;
final artifacts use the repository default.

Generated frontend assets, Wasm outputs and compatibility IDs must not be
committed. Core contains only shared backend resources; platform frontends are
generated under build directories. `.gitignore` also covers the former core
frontend paths to prevent accidentally reintroducing them.
