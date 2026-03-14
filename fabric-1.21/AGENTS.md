# FABRIC VERSION MODULE GUIDE

## OVERVIEW
This module is the Fabric version entrypoint pattern (`fabric-*`): provide version-specific initializer/wiring while reusing `fabric-helper` shared adapters.

## STRUCTURE
```text
fabric-1.21/
|- src/main/java/net/opanel/fabric_1_21/           # initializer + adapters/listeners
|- src/main/resources/fabric.mod.json              # mod metadata + entrypoints
`- build.gradle                                    # loom/shadow/remap pipeline
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Fabric server initializer | `src/main/java/net/opanel/fabric_1_21/Main.java` | `DedicatedServerModInitializer` lifecycle + web start |
| Version runtime bridge | `src/main/java/net/opanel/fabric_1_21/FabricServer.java` | Version-specific server binding |
| Version listeners/commands | `src/main/java/net/opanel/fabric_1_21/FabricListener.java` + `command/` | Platform-specific event/command glue |
| Mod metadata entrypoint | `src/main/resources/fabric.mod.json` | `entrypoints.server` points to module `Main` |
| Build and remap flow | `build.gradle` | Shadow `:core`, merge `:fabric-helper`, remap jar to `../build/libs` |

## CONVENTIONS
- Keep Fabric version deltas local to this module; move reusable logic into `fabric-helper`.
- Keep metadata (`fabric.mod.json`) and dependency versions aligned with gradle properties.

## ANTI-PATTERNS
- Do not copy shared wrapper logic from `fabric-helper` into version module classes.
- Do not introduce loader-agnostic core behavior here.

## NOTES
- Reuse this pattern for other `fabric-*` modules; only keep version-delta code in module-local classes.
- Keep `fabric.mod.json` entrypoint package synchronized with `Main.java` namespace for each version module.
