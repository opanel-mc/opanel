# Plugin/Mod Auto Update V2 Proposal

## Status

Draft for review.

V2 builds on the V1 multi-source auto update foundation. V1 already has a
provider coordinator, persistent bindings, background checks, Modrinth and
GitHub providers, and binding/status APIs. V2 turns that infrastructure into a
full update lifecycle: discover, identify, decide, apply, roll back, notify,
and audit.

## Goals

1. Add an in-browser marketplace search and plugin/mod install flow.
2. Add best-effort fuzzy identification for files that cannot be auto matched.
3. Replace the simple global policy with a per-plugin policy engine.
4. Keep a verified backup of the previous file so updates can be rolled back.
5. Notify server owners through in-panel messages, Discord webhooks, and other
   channels.
6. Support multi-server synchronization of update policies and bindings.
7. Make update history and security evidence auditable.

## Non-Goals

1. No automatic rollback after a plugin crash loop without explicit policy.
2. No support for every distribution site in V2.
3. No client-side auto update of the OPanel plugin itself.
4. No replacement of the platform's own update mechanisms.

## V1 Gaps That V2 Must Close

1. Finish the CurseForge provider in V1 before building V2 features on top of
   it.
2. Complete the V1 frontend binding dialog and settings section.
3. Add unit coverage for provider filtering, conflict detection, digest
   failures, and deferred apply propagation.
4. Keep provider coverage honest for manually verified integrations.

## Proposed Architecture

### New core pieces

1. `MarketplaceService`
   - searches provider projects
   - returns normalized project metadata and version lists
2. `PluginIdentityMatcher`
   - exact hash match first
   - fuzzy name/version fallback
   - stores confirmed identities as V1 bindings
3. `UpdatePolicyEngine`
   - global defaults plus per-plugin overrides
   - channel, version range, restart strategy, auto-apply window
4. `PluginUpdateStore`
   - update history
   - rollback snapshots
   - per-source credentials
5. `UpdateNotifier`
   - in-panel notification feed
   - webhook adapters
6. `RollbackManager`
   - keeps last verified jar before replacement
   - restores via the same safe replace path

### Coordinator evolution

V1 uses a linear provider list. V2 changes discovery into a pipeline:

1. `identify` - hash matching, bindings, then fuzzy matching
2. `discover` - query each matched source for compatible versions
3. `decide` - apply policy and rank candidates
4. `apply` - download, verify, safe replace, defer if locked
5. `record` - write history and rollback snapshot
6. `notify` - publish the result

## Data Model

### Binding v2

Add to `PluginUpdateBinding`:

1. `confidence` - `exact` or `fuzzy`
2. `lastMatchedAt`
3. `lastMatchVersion`
4. `policyOverrides`

### New storage files

1. `opanel/plugin-update-history.json`
2. `opanel/plugin-update-rollback/`
3. `opanel/plugin-update-notifications.json`

### Update history record

1. file name
2. source and project id
3. from version and to version
4. download URL and digest
5. result (`SUCCESS`, `DEFERRED`, `DIGEST_MISMATCH`, `ROLLED_BACK`, ...)
6. timestamp and server id

## Policy Model

### Global defaults

1. auto check
2. auto apply
3. restart strategy
4. channel preference
5. update window

### Per-plugin overrides

1. pinned version
2. excluded versions
3. source binding override
4. auto-apply flag
5. max delay before forced restart

## API Changes

1. `GET /api/plugins/marketplace/search?query=&platform=&gameVersion=`
2. `GET /api/plugins/marketplace/{source}/{projectId}`
3. `POST /api/plugins/install`
4. `GET /api/plugins/update-policies`
5. `POST /api/plugins/update-policies`
6. `GET /api/plugins/update-history`
7. `POST /api/plugins/rollback`
8. `GET /api/plugins/notifications`

## Frontend V2

1. Marketplace page with search filters and source badges.
2. Install flow with compatibility preview.
3. Plugin row actions for policy and rollback.
4. Update history table.
5. Notification center.

## Security and Safety

1. Credentials stay server-side and are encrypted at rest.
2. Fuzzy identity confirmation never auto-applies without user approval.
3. Rollback snapshots are retained until the next successful update.
4. Digest verification remains mandatory when the source exposes a digest.
5. All destructive operations use the existing safe replace path.

## Implementation Order

1. Complete V1 provider and frontend gaps.
2. Add update history and rollback snapshot storage.
3. Add policy engine and per-plugin overrides.
4. Add marketplace search service.
5. Add fuzzy identity matcher.
6. Add notification center and webhooks.
7. Add multi-server sync for bindings and policies.

## Verification Plan

1. Unit test policy precedence and version pinning.
2. Unit test rollback snapshot lifecycle.
3. Unit test fuzzy matcher precision and false-positive guard.
4. Manual test marketplace search for each V2 source.
5. Manual test notification delivery and failure states.

## Open Questions

1. Should fuzzy matching be opt-in or default for unknown jars?
2. Which notification channels are required for MVP: panel only, Discord, or
   email?
3. Should multi-server sync use a central instance or a file-based export?
4. Is automatic rollback after repeated startup failures acceptable?
