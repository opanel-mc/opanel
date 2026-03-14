# FOLIA VERSION MODULE GUIDE

## OVERVIEW
This module is the Folia entrypoint pattern (`folia-*`): integrate Folia-specific runtime with `core` while reusing shared Bukkit-family helper logic.

## STRUCTURE
```text
folia-1.21/
|- src/main/java/net/opanel/folia_1_21/            # entrypoint + Folia adapters/listeners
|- src/main/resources/plugin.yml                   # plugin metadata (`folia-supported: true`)
`- build.gradle                                    # helper merge + shading config
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Plugin bootstrap lifecycle | `src/main/java/net/opanel/folia_1_21/Main.java` | Folia plugin start/stop + web server boot |
| Version-specific server adapter | `src/main/java/net/opanel/folia_1_21/FoliaServer.java` | Folia runtime bridge to core interfaces |
| Version listener wiring | `src/main/java/net/opanel/folia_1_21/FoliaListener.java` | Event integration for this runtime |
| Plugin metadata | `src/main/resources/plugin.yml` | Includes `folia-supported: true` and entrypoint |
| Build merge behavior | `build.gradle` | Pulls in `:bukkit-helper` shared sources/resources |

## CONVENTIONS
- Keep Folia-specific scheduler/runtime behavior here; keep Bukkit-shared behavior in `bukkit-helper`.
- Keep lifecycle pattern consistent with other version modules (server binding then web start).

## ANTI-PATTERNS
- Do not duplicate shared Bukkit-family adapters in this module.
- Do not place generic core API/controller logic here.

## NOTES
- Apply this pattern to other `folia-*` modules: keep Folia-only runtime details local, shared logic in `bukkit-helper`.
- Verify plugin metadata keys when adding new Folia version modules (`main`, `api-version`, `folia-supported`).
