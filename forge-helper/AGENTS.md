# FORGE HELPER GUIDE

## OVERVIEW
`forge-helper` provides Forge-family shared adapters and metadata templates reused by `forge-*` modules.

## STRUCTURE
```text
forge-helper/
|- src/main/java/net/opanel/forge_helper/          # shared Forge adapters/config
|- src/main/resources/META-INF/mods.toml           # shared Forge metadata template
`- build.gradle                                    # ForgeGradle helper module config
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Shared server adapter behavior | `src/main/java/net/opanel/forge_helper/BaseForgeServer.java` | Common Forge-side `OPanelServer` behavior |
| Shared player/inventory wrappers | `src/main/java/net/opanel/forge_helper/BaseForge*.java` | Shared wrappers for forge version modules |
| Shared config classes | `src/main/java/net/opanel/forge_helper/config` | Forge config spec + config manager bridge |
| Shared mod metadata template | `src/main/resources/META-INF/mods.toml` | Injected into forge version jars during resource processing |

## CONVENTIONS
- Keep helper logic loader-shared across Forge versions; isolate version-specific entrypoint behavior to `forge-<version>` modules.
- Keep metadata/resources that are common across Forge versions in helper resources.
- Maintain stable adapter boundaries between `core` interfaces and Forge API surface.

## ANTI-PATTERNS
- Do not add `@Mod` entrypoint lifecycle code in helper; keep that in each `forge-*` module `Main.java`.
- Do not duplicate shared Forge adapter logic across many `forge-*` version modules.

## NOTES
- Forge version modules import helper resources during `processResources`, so template changes propagate broadly.
- Keep helper changes compatible across all maintained Forge version modules.
