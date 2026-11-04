# Performance incidents

Enable Additional diagnostics and Record performance incidents in Settings > Server, then restart. Both settings default off. Recording does not depend on an open browser.

Paper/Leaves feed the measured tick-end duration. Fabric, Forge and NeoForge measure between their matching server-tick start/end callbacks, excluding the wait between ticks. The default trigger is a one-second mean above 50 ms for ten consecutive seconds; five healthy seconds close an event.

The recorder preserves bounded samples before and after events, writes on the monitor worker, retains at most 100 events for seven days, and marks previously active records interrupted after restart. Stored events can focus the existing monitoring history.

Paper/Leaves additionally collect a bounded post-trigger scene around up to 64 players and 64 candidate chunks, without forced loading. Busy ticks skip collection; a 30-second window can end with missing or truncated evidence. Counts and player presence are not proof of cause. These chunk scene collectors have not yet been ported to the other Java backends.

Folia has no equivalent single-server tick source in this adapter; its incident switch is disabled rather than reporting global scheduler intervals as execution time. Pumpkin remains without this new incident recorder. These limitations are exposed, not silently replaced with TPS-derived timings.

Validation and remaining platform checks are documented in the contribution PR. No claim of zero overhead or full runtime validation across versions is made.
