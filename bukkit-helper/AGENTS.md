# BUKKIT HELPER GUIDE

## OVERVIEW
`bukkit-helper` holds shared Bukkit-family implementations reused by `spigot-*` and `folia-*` version modules.

## STRUCTURE
```text
bukkit-helper/
|- src/main/java/net/opanel/bukkit_helper/         # shared Bukkit adapters
|- src/main/resources/config.yml                   # shared default config resource
`- build.gradle                                    # helper dependency surface
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Shared server adapter behavior | `src/main/java/net/opanel/bukkit_helper/BaseBukkitServer.java` | Implements common `OPanelServer` behavior on Bukkit APIs |
| Player/inventory shared wrappers | `src/main/java/net/opanel/bukkit_helper/BaseBukkit*.java` | Cross-version wrappers used by version-specific subclasses |
| Shared config bridge | `src/main/java/net/opanel/bukkit_helper/config/ConfigManagerImpl.java` | Adapter between plugin config and core config contract |
| Shared command wiring | `src/main/java/net/opanel/bukkit_helper/command/OPanelCommand.java` | Common `/opanel` command behavior |
| Tick-thread bridge | `src/main/java/net/opanel/bukkit_helper/TaskRunner.java` | Sync task execution contract used by core calls |

## CONVENTIONS
- Keep this module version-agnostic within Bukkit family; rely on public Bukkit APIs that survive across supported versions.
- Keep package names under `net.opanel.bukkit_helper` and expose reusable base abstractions for version modules.
- Put only shared logic here; version-only NMS details belong in `spigot-*`/`folia-*` modules.

## ANTI-PATTERNS
- Do not add plugin entrypoint lifecycle code (`onEnable`, `onDisable`, server boot) in helper; keep it in version `Main.java`.
- Do not duplicate logic already implemented in `core`; helper is adapter layer, not business layer.

## NOTES
- Version modules usually merge helper sources/resources via their `build.gradle` (`compileJava` + `processResources` source wiring).
- Keep helper changes backward-compatible for all supported Bukkit-family version modules.
