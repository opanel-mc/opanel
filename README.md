<div align="center">

<img src="./images/brand.svg" width="300"/>

# FleetPanel

[![LICENSE](https://img.shields.io/badge/license-MPL_2.0-blue.svg "LICENSE")](./LICENSE)
[![Fork of](https://img.shields.io/badge/fork%20of-opanel--mc%2Fopanel-purple.svg)](https://github.com/opanel-mc/opanel)

> Self-hosted Minecraft multi-server management panel with Docker orchestration,
> granular permissions and historical monitoring.

</div>

## About

**FleetPanel** is a fork of [OPanel](https://github.com/opanel-mc/opanel) that
turns the original single-server panel into a full multi-server control plane.
Each Minecraft server runs inside its own Docker container; the web panel
provisions them from the UI, manages their lifecycle, monitors them over time,
and exposes everything through a granular permission system.

The Java plugin that runs inside the server (`net.opanel.*`) is still the one
from upstream OPanel — FleetPanel bundles it and talks to it over the same HTTP
API. The rest of the panel (Next.js frontend, PostgreSQL, Docker SDK, auth,
backups, scheduler, historical metrics) is the part that has been rebuilt for
this fork.

See [CONTRIBUTING.md](./CONTRIBUTING.md) for how to report bugs or propose
changes.

## What's different from upstream OPanel

| Area                  | Upstream OPanel                                  | FleetPanel                                                                    |
| --------------------- | ------------------------------------------------ | ----------------------------------------------------------------------------- |
| Server model          | Single server, panel talks to one plugin         | Multi-server, panel provisions and manages N Docker containers                |
| Hosting               | Plugin runs panel in-process                     | Panel runs stand-alone (Next.js + Postgres), plugin is only an agent          |
| Server creation       | Manual (install plugin by hand)                  | Create-server wizard in the UI, plugin is built and installed automatically   |
| Auth                  | Single access key                                | User accounts (bcrypt, JWT session), OWNER / ADMIN / USER roles               |
| Permissions           | All-or-nothing                                   | 40+ granular per-server permissions grouped by category                       |
| Monitoring            | Live only                                        | Historical metrics in Postgres, charts over time                              |
| Dashboard             | Fixed layout                                     | Draggable / resizable grid of panels, saved per user                          |
| Backups               | —                                                | Manual + scheduled world backups with restore                                 |
| Scheduled tasks       | —                                                | Cron-style scheduled console commands per server                              |
| Admin                 | —                                                | Admin panel: user CRUD, role assignment, per-server access grants             |
| i18n                  | en, zh-hk,zh-tw,zh-cn,jp,fr-fr(upcoming support),gm-de(upcoming support),korea(upcoming support)                                           | en, zh-cn, zh-tw, zh-hk (UI strings rebranded, upstream Chinese docs dropped) |

## Features

### Multi-server management

- **Create servers from the UI** — wizard picks server type
  (Paper / Spigot / Fabric / Forge / NeoForge), Minecraft version, memory,
  Java version, game / RCON / plugin ports, data path.
- **Docker orchestration** — each server runs in its own container; the panel
  pulls images, builds and installs the plugin, creates containers, wires
  volumes and exposes ports automatically.
- **Lifecycle controls** — start, stop, restart, delete, rename, from a single
  server list.
- **Provisioning progress** — a dedicated setup page polls status
  (pulling → building plugin → creating → starting → ready) with a progress bar
  and error surfacing.

### Dashboard

- **Customizable grid** — `react-grid-layout` lets users drag and resize
  panels; layout is persisted per user, per server in `localStorage`.
- **Panels** — Server info (name, type, version, port, memory, Java, icon,
  MOTD, start/stop/restart), CPU & RAM, system stats, TPS, uptime, online
  players, live console.
- **Live + historical monitoring** — current readings plus charts backed by
  the `ServerMetric` Postgres table (CPU %, memory, TPS, player count), with
  CPU normalized to 0–100 even on multi-core hosts.
- **Server icon & MOTD inline editing** — click the icon to upload a PNG,
  click the MOTD pencil to edit; color-code preview uses the same renderer
  as the console.

### Console & terminal

- **Interactive terminal page** — full-screen console with command history.
- **Formatting codes** — unified pipeline handles both Minecraft `§` codes
  and raw ANSI escape sequences, so colored output from Paper / Fabric
  loggers renders correctly.
- **Tab completion hints** and scroll-locked live view.

### Players & whitelist

- Online / offline player lists with avatar, gamemode, latency.
- Kick, ban, pardon, change gamemode, op / deop.
- View and edit player inventory (NBT-aware).
- Whitelist viewing and management.

### Worlds (saves)

- List, upload, download, delete and enable worlds.
- World directory is mounted directly into the server container.

### Gamerules editor

- Toggle gamerules without typing commands.
- **MC 26.1 compatibility** — auto-detects the server's naming format
  (`keepInventory` vs `keep_inventory`) via RCON and uses the correct form.
- Error surface with retry when the server is unreachable.

### Plugins / Mods

- Enable, disable, upload and delete plugins (Bukkit family).
- View detailed plugin information.

### Logs

- Browse and download server log files.
- Permission-gated delete.

### Scheduled tasks

- Cron expressions per server (backed by `node-cron`).
- Execute one or more console commands per run.
- Enable / disable individual tasks without deleting them.

### Backups

- Manual and automatic (scheduled) backups of world saves.
- Restore a backup back into a server.
- Backup metadata (size, type, notes) tracked in Postgres.

### Settings & config files

- Monaco-based editor for `server.properties` and other configs.
- Panel-level settings: theme, login banner, access key rotation.

### Authentication, roles & permissions

- **User accounts** stored in Postgres, passwords hashed with bcrypt,
  JWT session cookies (`jose`).
- **Global roles**: `OWNER`, `ADMIN`, `USER`. OWNER / ADMIN bypass all
  permission checks.
- **40+ granular per-server permissions** grouped by category:
  Server lifecycle, Console, Players, Whitelist, Plugins, Saves, Logs,
  Tasks, Gamerules, Settings, Icon & MOTD, Monitoring, Backups.
- **Permission-aware UI** — sidebar hides sections the user can't see,
  dashboard controls (icon, MOTD, start/stop/restart) disable when the user
  lacks the right, direct visits to gated routes render a "No access" page.
- **Dashboard landing fallback** — users without `monitor.view` are
  auto-redirected to the first sidebar section they can access.

### Admin panel

- User CRUD (create, delete, reset password, toggle role).
- Per-server access grants: assign granular permissions to a user for a
  specific server.
- Panel-wide settings tab (login banner, security, update check).

## Supported Minecraft versions

The Java plugin core targets these loaders / versions (see [`plugin/`](./plugin/)):

- **Paper / Spigot**: 1.16.1, 1.19.4, 1.20, 1.20.5, 1.21, 1.21.9, 26.1
- **Fabric**: 1.19, 1.19.4, 1.20, 1.20.2, 1.20.3, 1.20.5, 1.21, 1.21.2,
  1.21.5, 1.21.9, 1.21.11, 26.1
- **Forge**: 1.19.4, 1.20.1, 1.20.2, 1.20.3, 1.20.6, 1.21, 1.21.3, 1.21.5,
  1.21.8, 1.21.9, 1.21.11, 26.1
- **Folia**: 1.20, 1.20.5, 1.21, 1.21.11
- **NeoForge**: 1.21.1

## Tech stack

**Frontend**: Next.js 15 (App Router, Turbopack), React 19, TypeScript,
Tailwind CSS 4, Shadcn UI, Lucide, Monaco editor, recharts,
react-grid-layout.

**Backend** (Next.js route handlers): PostgreSQL + Prisma, dockerode,
rcon-client, node-cron, jose (JWT), bcryptjs.

**Plugin core** (inside each Minecraft server): Java, Javalin, Item-NBT-API.

## Quick start (Docker)

Requirements: Docker + Docker Compose, a host that can run Linux containers.

```bash
git clone https://github.com/kojikk/fleetpanel
cd fleetpanel
docker compose up -d
```

The panel will be available at `http://localhost:3001`. Managed Minecraft
servers run as sibling containers; their world data is mounted under
`./servers/<serverId>` on the host.

Before exposing the panel to the internet you **must** override
`JWT_SECRET` and the Postgres credentials in [`docker-compose.yml`](./docker-compose.yml).

### Development

```bash
cd frontend
pnpm install
pnpm db:push        # create the Postgres schema
pnpm dev            # Next.js dev server on :3001
```

See [`dev.bat`](./dev.bat) on Windows.

## Credits

- **[OPanel](https://github.com/opanel-mc/opanel)** by Norcleeh — the upstream
  project and the source of the Java plugin core this fork still reuses.
- **[Claude](https://claude.ai/code)** (Anthropic) — coding agent used during
  the development of this fork to help design and implement the Docker
  orchestration layer, permission system, admin panel, historical monitoring,
  backups and dashboard rewrite.
- Every open-source dependency listed on the panel's `/about/thanks` page.

## License

[MPL-2.0](./LICENSE) — the same license as upstream OPanel.

Per MPL § 3.4 the upstream copyright notices and license headers are preserved
in this fork; new files added by FleetPanel are released under the same MPL-2.0.
