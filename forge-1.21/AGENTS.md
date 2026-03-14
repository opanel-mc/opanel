# FORGE VERSION MODULE GUIDE

## OVERVIEW
This module is the Forge version entrypoint pattern (`forge-*`): wire Forge lifecycle/events into `core` and merge shared code from `forge-helper`.

## STRUCTURE
```text
forge-1.21/
|- src/main/java/net/opanel/forge_1_21/            # @Mod entrypoint + adapters/listeners
|- src/main/resources/pack.mcmeta                  # module-local resources
`- build.gradle                                    # ForgeGradle + helper resource merge
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Forge entrypoint lifecycle | `src/main/java/net/opanel/forge_1_21/Main.java` | `@Mod` class handles server start/stop/tick wiring |
| Version runtime bridge | `src/main/java/net/opanel/forge_1_21/ForgeServer.java` | Version-specific server adapter |
| Version listener/command glue | `src/main/java/net/opanel/forge_1_21/ForgeListener.java` + `command/` | Platform event and command integration |
| Module resources | `src/main/resources/pack.mcmeta` | Forge module-specific resource in this directory |
| Shared metadata/template source | `forge-helper/src/main/resources/META-INF/mods.toml` | Pulled in via `build.gradle` `from project(":forge-helper").sourceSets.main.resources` |

## CONVENTIONS
- Keep version-specific Forge API adjustments here; keep reusable adapter logic in `forge-helper`.
- Keep resource merge assumptions explicit in build config when touching metadata/templates.

## ANTI-PATTERNS
- Do not duplicate shared config/adapter code already available in `forge-helper`.
- Do not move route/controller business behavior from `core` into this module.

## NOTES
- For other `forge-*` modules, follow the same split: version glue here, reusable logic/templates in `forge-helper`.
- If metadata behavior changes, verify both module-local resources and helper-provided `mods.toml` expansion.
