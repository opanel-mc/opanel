# FABRIC HELPER GUIDE

## OVERVIEW
`fabric-helper` contains Fabric-shared adapters and support code reused by `fabric-*` version modules.

## STRUCTURE
```text
fabric-helper/
|- src/main/java/net/opanel/fabric_helper/         # shared Fabric adapters/events/mixins
|- src/main/resources/opanel.mixin.json            # shared mixin declaration
`- build.gradle                                    # loom + helper wiring
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Shared server adapter behavior | `src/main/java/net/opanel/fabric_helper/BaseFabricServer.java` | Common `OPanelServer` implementation on Fabric server APIs |
| Shared player/inventory wrappers | `src/main/java/net/opanel/fabric_helper/BaseFabric*.java` | Reusable wrappers for version modules |
| Shared config adapter | `src/main/java/net/opanel/fabric_helper/config/ConfigManagerImpl.java` | ConfigX bridge used by Fabric entrypoints |
| Shared event hook support | `src/main/java/net/opanel/fabric_helper/event` | Fabric-side shared event behavior |
| Shared mixin declarations | `src/main/resources/opanel.mixin.json` + `src/main/java/net/opanel/fabric_helper/mixin` | Mixin hooks reused by version modules |

## CONVENTIONS
- Keep code compatible across all supported Fabric versions; isolate version-specific deltas in `fabric-<version>` modules.
- Keep mod metadata/resource templates centralized when they are shared (`assets/opanel`, mixin config).
- Keep helper abstractions aligned with `core` contracts, not route/page-level frontend concerns.

## ANTI-PATTERNS
- Do not place DedicatedServerModInitializer bootstrap in helper; that belongs to version `Main.java`.
- Do not hardcode one Minecraft version inside helper logic that should serve multiple Fabric versions.

## NOTES
- `fabric-helper` exposes shared resources (icon/mixin config) consumed by version module resource processing.
- Keep mixin changes reviewed against all active Fabric version modules, not only one.
