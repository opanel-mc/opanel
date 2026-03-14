# NEOFORGE VERSION MODULE GUIDE

## OVERVIEW
This module is the NeoForge version entrypoint pattern (`neoforge-*`): module-local NeoForge bootstrap and version wiring around shared `core` behavior.

## STRUCTURE
```text
neoforge-1.21.1/
|- src/main/java/net/opanel/neoforge_1_21_1/       # entrypoint + Neo adapters/listeners
|- src/main/resources/META-INF/neoforge.mods.toml  # NeoForge metadata and deps
`- build.gradle                                    # userdev + shadow packaging flow
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| NeoForge entrypoint lifecycle | `src/main/java/net/opanel/neoforge_1_21_1/Main.java` | Startup/shutdown and server tick integration |
| Version runtime bridge | `src/main/java/net/opanel/neoforge_1_21_1/NeoServer.java` | Binds runtime to core server contract |
| Version listener/command glue | `src/main/java/net/opanel/neoforge_1_21_1/NeoListener.java` + `command/` | Runtime event and command wiring |
| NeoForge metadata | `src/main/resources/META-INF/neoforge.mods.toml` | Module-local mod metadata and dependency ranges |
| Build/shadow output behavior | `build.gradle` | Shadows `:core` and emits build jar to `../build/libs` |

## CONVENTIONS
- Keep NeoForge-specific API behavior isolated here.
- Keep shared business and protocol logic in `core`; this module should stay adapter-oriented.

## ANTI-PATTERNS
- Do not copy shared Forge-family helper logic into this module unless it is truly NeoForge-only.
- Do not scatter metadata rules across Java code when `neoforge.mods.toml` is the source of truth.

## NOTES
- Use this as the pattern for other `neoforge-*` modules: keep Neo-specific API wiring local, preserve core boundaries.
- Keep metadata dependency ranges aligned with module gradle properties when adding new NeoForge versions.
