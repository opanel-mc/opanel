# Map player positions

Opt-in player avatar markers and an online-player list share one demand-driven snapshot. Enable Additional diagnostics in Settings > Server, restart, then enable Player positions in map settings. The upper-right list locates players in the selected map world; it never teleports players in-game.

Positions are sampled at most every five seconds while a viewer requests them. Idle collection stops after the viewer lease expires. Hidden browser tabs stop polling. Stale snapshots are not presented as current.

Paper/Leaves read players on the game thread. Fabric, Forge and NeoForge publish through their existing server-thread callbacks. Folia dispatches reads to each player's entity scheduler and publishes a bounded-time batch from the global scheduler. No player location is read from the HTTP thread.

Overworld, Nether and End use separate map keys, height ranges, caches and dirty queues. Nether rendering skips the ceiling to the next surface below its air gap. Cached and live tiles use the same surface policy. The primary world is the default map selection.

Pumpkin remains without map support, matching the original backend. The experimental live chunk-load monitor is not part of this contribution branch.
