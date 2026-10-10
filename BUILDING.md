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
frontend_env_texture_versions=26.1,26.2,26.3
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

Next.js uses `output: "export"` and exports static files to `frontend/out`,
with intermediate files and caches in `frontend/.next`. The build script moves
the export to `frontend/dist/client`, copies the Next.js build ID into the
existing compatibility-ID resource, then
publishes the complete output to the module directory. A shared Gradle service
serializes frontend tasks within one Gradle invocation, including publication;
Java compilation can still run in parallel. Do not run separate frontend builds
simultaneously in the same checkout.

Each module packages `client/` as `opanel-web/` and places the matching
`vinext-rsc-compatibility-id` at the JAR resource root. This legacy resource name
is retained for backend compatibility; its contents are the Next.js build ID,
which also matches the `x-nextjs-deployment-id` response header. Final JARs remain under
the root `build/libs`, for example `opanel-paper-26.1-build-2.2.4.jar`.
Inventory textures are bundled only for the configured versions; map assets and
Minecraft translations retain their shared generation strategy.

## Pumpkin

Pumpkin does not use Gradle. Install frontend dependencies externally, then
invoke Cargo from the repository root:

```sh
npm --prefix frontend ci
cargo build --release --locked
```

Windows PowerShell:

```powershell
npm.cmd --prefix frontend ci
cargo build --release --locked
```

Configure the frontend in `pumpkin/frontend.properties`:

```properties
VITE_OPANEL_TARGET=pumpkin-26.3
TEXTURE_VERSIONS=26.3
```

Use UTF-8 `key=value` entries, one per line. Lines starting with `#` or `!` are
comments. Property names are the environment variable names directly, preferably
written in uppercase. The asset crate's Cargo build script also normalizes names
to uppercase and passes values unchanged to `npm run build`. Additional entries
are forwarded automatically. Both `VITE_OPANEL_TARGET` and `TEXTURE_VERSIONS`
must be configured for frontend compilation. Values are literal, without
properties escape or line-continuation processing.

Cargo checks for installed frontend tools but never installs npm dependencies.
Its build script runs frontend compilation and embeds the resulting client files
and compatibility ID into the plugin. It sets `OPANEL_FRONTEND_OUTPUT` to
`<OUT_DIR>/frontend` under Cargo's build directory, so it does not consume a
previous build left in `frontend/dist`. That directory is used to stage the
completed static export before publication. Keep frontend builds in the same checkout
sequential, including builds started through Gradle and Cargo.
On Windows, a custom `CARGO_TARGET_DIR` must stay on the same drive as the
repository because compressed resource embedding requires relative paths.

Changes to frontend sources or `pumpkin/frontend.properties` trigger a rebuild;
subsequent Rust-only builds can reuse the generated frontend. Cargo commands
that compile the asset crate, including `cargo check` and `cargo test`, also
require frontend dependencies. Local builds prepare Minecraft resources and Wasm
automatically. With `OPANEL_FRONTEND_PREPARED=1`, they validate and reuse prepared
resources while still compiling the target frontend. Wasm preparation uses its
own Cargo target directory to avoid nesting builds in Pumpkin's target directory.

## Development and verification

`npm --prefix frontend run dev` defaults the target to `paper`.
Production builds require a nonempty `VITE_OPANEL_TARGET`. Its value is compiled
into the frontend; changing server runtime environment variables does not change
an existing build. The existing `VITE_OPANEL_TARGET` and `VITE_OPANEL_VERSION`
inputs are retained for Gradle/Cargo compatibility and mapped by `next.config.ts`
to `NEXT_PUBLIC_OPANEL_TARGET` and `NEXT_PUBLIC_OPANEL_VERSION`.
`npm --prefix frontend start` serves the static export on port 3001.

### Inventory texture versions

`TEXTURE_VERSIONS` is read only by the build tools. Java forwards it from
`frontend_env_texture_versions`; Pumpkin reads it from `frontend.properties`.
For development, set it in `frontend/.env.development`, for example:

```dotenv
VITE_OPANEL_TARGET=paper-26.1
TEXTURE_VERSIONS=all
```

- Set a single texture version or a comma-separated list, such as
  `1.21.2,1.21.4`. Whitespace is trimmed, duplicates are removed, and versions are
  sorted numerically. Values must exist in the installed `minecraft-textures`
  catalog: MC 1.20.2 uses texture version `1.20`, so `1.20.2` is not a valid value.
- `all` includes every catalog version, including 1.12 through 1.18, but never
  the `*.id.json` files. Development defaults to `all` if the setting is missing
  or empty. Production builds require an explicit value and also accept `all`.
- Unknown versions, version ranges, empty list entries, mixing `all` with a
  version list, and missing selected JSON files fail validation. Restart the
  development server after changing the environment configuration.
- Environment files use Next.js loading rules; process environment
  variables take precedence. `npm run build` validates the selection before its
  own prelaunch step and forwards the normalized list to Next.js. Gradle's shared
  resource preparation still runs before its target frontend task.

The Next.js configuration generates `frontend/build/textures.js` with literal
dynamic imports only for selected texture JSONs,
keeping them lazy-loaded and excluding all other texture versions from the
bundle. At runtime, the server's Minecraft version still determines the required
texture version. If that version was not bundled, texture loading returns `null`
instead of falling back to a different bundled version.

The initial platform selections follow the
[supported-version list](https://dist.opanel.cn/supported-version-list.json): each
supported MC version maps to the latest texture version not newer than itself,
then the results are deduplicated. For example, `folia-1.21` selects
`1.21.4,1.21.5,1.21.6`, while `neoforge-1.21.1` selects `1.21,1.21.2`. These lists
are checked into each target's properties file; builds do not fetch the remote
list or infer selections from module names. Update the explicit lists whenever
platform compatibility or the texture dependency changes. Existing Gradle inputs
and Cargo property-file tracking ensure configuration changes rebuild the frontend.

Local checks without a full frontend or Gradle build:

```sh
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run test -- vite-plugins/textures-plugin.test.ts
node --test .github/scripts/build-matrix.test.mjs
```

### Comparing Next.js and vinext build times

The Next.js version is pinned in `frontend/package.json` and uses Turbopack by
default. Vite remains a development dependency for Vitest only. The resource
preparation, target variables, texture selection and Gradle task serialization
are unchanged, so the same full-build command can be measured on each revision.
Install dependencies before timing each revision and use the same Node.js/JDK
versions, Gradle options and prepared-resource state.

For example, from the repository root in PowerShell:

```powershell
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend run prelaunch
$env:OPANEL_FRONTEND_PREPARED = "1"
Measure-Command {
    .\gradlew.bat clean build --no-build-cache --console=plain
    if ($LASTEXITCODE -ne 0) { throw "OPanel build failed" }
}
Remove-Item Env:OPANEL_FRONTEND_PREPARED
```

This measures all Java targets with Minecraft/Wasm preparation completed before
timing; Pumpkin is built separately with Cargo. `clean` invalidates module outputs
but does not remove frontend framework caches. For a cold frontend comparison,
also clear `frontend/.next`, `frontend/.vinext`, `frontend/.vite` and
`frontend/node_modules/.vite` before each measurement. For a warm comparison,
retain those caches, warm up each revision and report repeated measurements.
Dependency download and Gradle daemon state should be consistent between runs.
Next.js's normal production build also includes TypeScript validation.

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
| Pumpkin code, `pumpkin/frontend.properties` or root Cargo manifest/lockfile | Skip | Build |
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

The final `ci` job aggregates all job results and provides a stable required
status check for branch rulesets. It runs even after failures or skips and fails
if any dependency failed or was cancelled. Successful and skipped jobs are
accepted; this check does not separately validate the build selection outputs.

The Java matrix is generated from the module registry (currently 39 targets)
and filtered to the selected targets. Each job installs npm dependencies in a
separate workflow step before invoking Gradle, downloads the shared inputs,
sets `OPANEL_FRONTEND_PREPARED=1`, and builds and uploads its own JAR. Automatic
runs only upload affected targets; they do not mix in artifacts from older runs.
Use manual `build_jar` / `build_pumpkin` inputs to force complete builds of the
enabled branches. Selecting neither skips preparation, checks and builds after
detection. Matrix failures do not cancel other targets; GitHub's available
concurrency determines scheduling.

The Pumpkin check job and all five native target jobs install npm dependencies
and download the shared prepared inputs before invoking Cargo. Each sets
`OPANEL_FRONTEND_PREPARED=1`; Cargo builds and embeds its own frontend using
`pumpkin/frontend.properties`. There is no separate Pumpkin frontend build job
or compiled frontend artifact. Shared preparation artifacts are retained for
seven days; final artifacts use the repository default.

Generated frontend assets, Wasm outputs and compatibility IDs must not be
committed. Core contains only shared backend resources; platform frontends are
generated under build directories. `.gitignore` also covers the former core
frontend paths to prevent accidentally reintroducing them.
