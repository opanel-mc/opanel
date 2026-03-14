# SPIGOT VERSION MODULE GUIDE

## OVERVIEW
This module is the Bukkit-family version entrypoint pattern (`spigot-*`): wire platform lifecycle/events to `core` using `bukkit-helper` abstractions.

## STRUCTURE
```text
spigot-1.21/
|- src/main/java/net/opanel/spigot_1_21/           # version entrypoint + adapters/listeners
|- src/main/resources/plugin.yml                   # plugin metadata and command declaration
`- build.gradle                                    # shadow + helper source/resource merge
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Plugin bootstrap lifecycle | `src/main/java/net/opanel/spigot_1_21/Main.java` | `onEnable`/`onDisable` + web server startup |
| Version-specific server adapter | `src/main/java/net/opanel/spigot_1_21/SpigotServer.java` | Binds Bukkit runtime to core contracts |
| Version listener wiring | `src/main/java/net/opanel/spigot_1_21/SpigotListener.java` | Platform event bridge |
| Plugin metadata/entrypoint | `src/main/resources/plugin.yml` | `main: net.opanel.spigot_1_21.Main`, command declaration |
| Version dependencies/shading | `build.gradle` | Includes `:core` shadow + `:bukkit-helper` sources/resources |

## CONVENTIONS
- Keep this module focused on version-specific wiring and API deltas.
- Reuse shared logic from `bukkit-helper`; only keep code here that cannot be shared across Bukkit-family versions.
- Keep startup behavior consistent with other version modules: set server, then start web server.

## ANTI-PATTERNS
- Do not move generic Bukkit-family logic into this module when it can live in `bukkit-helper`.
- Do not add panel business rules here; keep domain logic in `core`.

## NOTES
- Apply this same split to other `spigot-*` modules: helper for shared code, module for version-specific glue.
- Keep `plugin.yml` `main` path aligned with the package/version namespace when cloning to a new version module.
