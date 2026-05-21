package net.opanel.map;

import net.opanel.OPanel;
import net.opanel.common.OPanelChunkAccessor;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWorldRegion;
import net.opanel.config.MapConfiguration;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelChunkDirtyEvent;
import net.opanel.event.OPanelDirtyChunksFlushEvent;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MapRenderManager {
    private static final String OTILES_SUFFIX = ".otiles";
    private static final String OTILES_TMP_SUFFIX = ".otiles.tmp";

    private static final long FLUSH_INTERVAL_MS = 5000L;
    private static final int MAX_CHUNKS_PER_FLUSH = 64;
    private static final long BUNDLE_WRITE_DEBOUNCE_MS = 5000L;

    private final OPanel plugin;
    private final MapConfiguration config;
    private final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "opanel-map-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, List<OPanelWorldRegion>> saveRegionMap = new HashMap<>();

    private final Map<String, Set<Long>> availableTilesIndex = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, byte[]>> tileBytesCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> indexVersion = new ConcurrentHashMap<>();

    private Consumer<OPanelChunkDirtyEvent> chunkDirtyListener;
    private final DirtyChunkTracker dirtyChunkTracker = new DirtyChunkTracker();
    private final Map<String, ScheduledFuture<?>> pendingBundleWrite = new ConcurrentHashMap<>();

    public MapRenderManager(OPanel plugin) {
        this.plugin = plugin;
        config = Storage.get().getStoredData(StorageKey.MAP_CONFIG);
    }

    public static long packCoord(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    public void init() {
        if(config == null || !config.enabled) return;

        for(OPanelSave save : plugin.getServer().getSaves()) {
            if(!save.isRunning()) continue; // skip the saves that is not running on the server
            saveRegionMap.put(save.getName(), save.getRegions());
        }

        Set<String> bundleNames = getTileBundleNames();

        for(Map.Entry<String, List<OPanelWorldRegion>> entry : saveRegionMap.entrySet()) {
            String saveName = entry.getKey();
            if(bundleNames.contains(saveName)) {
                executor.execute(() -> loadTileBundle(saveName));
            } else {
                renderSave(saveName, entry.getValue())
                    .thenRunAsync(() -> writeTileBundle(saveName), executor)
                    .thenRunAsync(() -> plugin.logger.info("Save '"+ saveName +"' is pre-rendered successfully"));
            }
        }

        scheduler.scheduleWithFixedDelay(
                this::flushDirtyChunks,
                FLUSH_INTERVAL_MS,
                FLUSH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );

        chunkDirtyListener = (OPanelChunkDirtyEvent e) -> {
            dirtyChunkTracker.markDirty(e.getChunkX(), e.getChunkZ());
        };

        EventManager.get().on(EventType.CHUNK_DIRTY, chunkDirtyListener);
    }

    private void flushDirtyChunks() {
        OPanelServer server = plugin.getServer();
        OPanelChunkAccessor accessor = server.getChunkAccessor();
        if(accessor == null) return;

        List<Long> batch = dirtyChunkTracker.drain(MAX_CHUNKS_PER_FLUSH);
        if(batch.isEmpty()) return;

        final String saveName = server.getCurrentSaveName();
        Set<int[]> flushedChunks = new HashSet<>();
        for(long packed : batch) {
            final int chunkX = unpackX(packed);
            final int chunkZ = unpackZ(packed);
            final Tile tile = accessor.readLiveTile(chunkX, chunkZ);
            if(tile == null) continue;

            renderTile(saveName, chunkX, chunkZ, tile)
                    .thenRunAsync(() -> scheduleBundleWrite(saveName), executor);
            flushedChunks.add(new int[] { chunkX, chunkZ });
        }
        EventManager.get().emit(EventType.DIRTY_CHUNKS_FLUSH, new OPanelDirtyChunksFlushEvent(saveName, flushedChunks));
    }

    /**
     * Coalesce bundle writes for a save into at most one write every
     * {@link #BUNDLE_WRITE_DEBOUNCE_MS}. Multiple calls within the window
     * collapse to a single delayed write.
     */
    public void scheduleBundleWrite(String saveName) {
        pendingBundleWrite.computeIfAbsent(saveName, key ->
            scheduler.schedule(() -> {
                pendingBundleWrite.remove(key);
                try {
                    writeTileBundle(key);
                } catch (Throwable e) {
                    plugin.logger.warn("Failed to write tile bundle for save '"+ key +"': "+ e.getMessage());
                }
            }, BUNDLE_WRITE_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
        );
    }

    private Set<String> getTileBundleNames() {
        Set<String> bundleNames = new HashSet<>();
        Path mapDataDir = OPanel.MAP_DATA_PATH;
        if(!Files.isDirectory(mapDataDir)) return bundleNames;

        try(Stream<Path> stream = Files.list(mapDataDir)) {
            stream.forEach(path -> {
                if(!Files.isRegularFile(path)) return;

                String fileName = path.getFileName().toString();
                if(!fileName.endsWith(OTILES_SUFFIX)) return;

                String saveName = fileName.substring(0, fileName.length() - OTILES_SUFFIX.length());
                bundleNames.add(saveName);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bundleNames;
    }

    private void loadTileBundle(String saveName) {
        long start = System.currentTimeMillis();
        Path bundlePath = OPanel.MAP_DATA_PATH.resolve(saveName + OTILES_SUFFIX);
        try {
            byte[] data = Files.readAllBytes(bundlePath);
            Map<Long, byte[]> parsed = TileCompressor.parseBundle(data);

            Map<Long, byte[]> bytesMap = new ConcurrentHashMap<>(parsed);
            Set<Long> coords = ConcurrentHashMap.newKeySet();
            coords.addAll(parsed.keySet());

            tileBytesCache.put(saveName, bytesMap);
            availableTilesIndex.put(saveName, coords);
            indexVersion.computeIfAbsent(saveName, k -> new AtomicLong(0L)).incrementAndGet();

            long elapsed = System.currentTimeMillis() - start;
            plugin.logger.info("Loaded "+ parsed.size() +" tiles for save '"+ saveName +"' in "+ elapsed +"ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTileBundle(String saveName) {
        Map<Long, byte[]> bytesMap = tileBytesCache.get(saveName);
        if(bytesMap == null || bytesMap.isEmpty()) return;

        Map<Long, byte[]> snapshot = new HashMap<>(bytesMap);
        try {
            byte[] payload = TileCompressor.bundleTiles(snapshot).toByteArray();
            Path tmp = OPanel.MAP_DATA_PATH.resolve(saveName + OTILES_TMP_SUFFIX);
            Path dst = OPanel.MAP_DATA_PATH.resolve(saveName + OTILES_SUFFIX);
            Files.write(tmp, payload);
            Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<?> renderSave(String saveName, List<OPanelWorldRegion> regions) {
        if(regions.isEmpty()) return CompletableFuture.completedFuture(null);

        CompletableFuture<?>[] arr = regions.stream()
            .map(region -> CompletableFuture.runAsync(new TilesRenderTask(plugin, saveName, region), executor))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(arr);
    }

    public CompletableFuture<?> renderTile(String saveName, int chunkX, int chunkZ, Tile tile) {
        return CompletableFuture.runAsync(new SingleTileRenderTask(plugin, saveName, chunkX, chunkZ, tile), executor);
    }

    public void submitRenderedTile(String saveName, int x, int z, byte[] bytes) {
        long packed = packCoord(x, z);
        availableTilesIndex
            .computeIfAbsent(saveName, k -> ConcurrentHashMap.newKeySet())
            .add(packed);
        tileBytesCache
            .computeIfAbsent(saveName, k -> new ConcurrentHashMap<>())
            .put(packed, bytes);
        indexVersion
            .computeIfAbsent(saveName, k -> new AtomicLong(0L))
            .incrementAndGet();
    }

    public Set<Long> getAvailableTileCoords(String saveName) {
        Set<Long> set = availableTilesIndex.get(saveName);
        if(set == null) return Collections.emptySet();
        return new HashSet<>(set); // snapshot to avoid concurrent modification during iteration
    }

    public long getIndexVersion(String saveName) {
        AtomicLong v = indexVersion.get(saveName);
        return v == null ? 0L : v.get();
    }

    public boolean hasSave(String saveName) {
        return availableTilesIndex.containsKey(saveName);
    }

    /**
     * Returns cached tile bytes for the given coord, or null if not yet rendered/loaded.
     * During startup, the per-save bundle is loaded asynchronously — callers may see a
     * transient null until the load completes; the frontend re-polls as
     * {@link #getIndexVersion(String)} advances.
     */
    public byte[] loadTileBytes(String saveName, int x, int z) {
        Map<Long, byte[]> bytesMap = tileBytesCache.get(saveName);
        if(bytesMap == null) return null;
        return bytesMap.get(packCoord(x, z));
    }

    public void shutdown() {
        scheduler.shutdownNow();
        // Flush any pending bundle writes synchronously so we don't lose
        // the latest realtime updates if the server is shutting down mid-debounce.
        for(String saveName : new HashSet<>(pendingBundleWrite.keySet())) {
            ScheduledFuture<?> pending = pendingBundleWrite.remove(saveName);
            if(pending != null) pending.cancel(false);
            try {
                writeTileBundle(saveName);
            } catch (Throwable e) {
                plugin.logger.warn("Failed to flush tile bundle on shutdown for save '"+ saveName +"': "+ e.getMessage());
            }
        }
        executor.shutdownNow();

        saveRegionMap.clear();
        tileBytesCache.clear();
        indexVersion.clear();
        if(chunkDirtyListener != null) {
            EventManager.get().off(EventType.CHUNK_DIRTY, chunkDirtyListener);
        }
    }
}
