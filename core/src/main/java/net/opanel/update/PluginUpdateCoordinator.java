package net.opanel.update;

import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.exception.ActLaterException;
import net.opanel.exception.PluginUpdateConflictException;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginUpdateCoordinator {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final long PROVIDER_TIMEOUT_SECONDS = 45;
    private static final Logger LOGGER = Logger.getLogger(PluginUpdateCoordinator.class.getName());
    private static final ExecutorService CHECK_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "opanel-plugin-update-source-checker");
        thread.setDaemon(true);
        return thread;
    });

    private final List<UpdateSourceProvider> providers;
    private final PluginUpdateConfigStore configStore;
    private final long cacheDurationMs;

    private volatile long lastCheckedAt = 0;
    private volatile List<PluginUpdate> cachedUpdates = Collections.emptyList();
    private volatile Map<String, String> cachedFileHashes = Collections.emptyMap();
    private volatile String cachedServerVersion;
    private volatile ServerType cachedServerType;

    public PluginUpdateCoordinator(List<UpdateSourceProvider> providers, long checkIntervalSeconds) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
        this.configStore = new PluginUpdateConfigStore();
        this.cacheDurationMs = Math.max(checkIntervalSeconds, 0) * 1000L;
    }

    public synchronized List<PluginUpdate> check(
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        String serverVersion,
        ServerType serverType,
        boolean force
    ) throws IOException {
        final Map<String, String> fileHashes = calculateFileHashes(pluginsPath, plugins);
        if(
            !force
            && System.currentTimeMillis() - lastCheckedAt < cacheDurationMs
            && Objects.equals(cachedServerVersion, serverVersion)
            && cachedServerType == serverType
            && cachedFileHashes.equals(fileHashes)
        ) {
            return new ArrayList<>(cachedUpdates);
        }

        final LinkedHashMap<String, PluginUpdate> updateMap = new LinkedHashMap<>();
        final PluginUpdateConfig config = configStore.getConfig();
        List<CompletableFuture<List<PluginUpdate>>> checks = new ArrayList<>();
        for(UpdateSourceProvider provider : providers) {
            checks.add(CompletableFuture.supplyAsync(
                () -> checkProvider(provider, pluginsPath, plugins, fileHashes, serverVersion, serverType, config),
                CHECK_EXECUTOR
            ).orTimeout(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(error -> {
                LOGGER.log(Level.WARNING, "Update source [" + provider.getSource() + "] timed out; skipping it", error);
                return List.of();
            }));
        }
        for(CompletableFuture<List<PluginUpdate>> check : checks) {
            for(PluginUpdate update : check.join()) {
                updateMap.putIfAbsent(update.getFileName(), update);
            }
        }

        List<PluginUpdate> updates = new ArrayList<>(updateMap.values());
        lastCheckedAt = System.currentTimeMillis();
        cachedUpdates = Collections.unmodifiableList(new ArrayList<>(updates));
        cachedFileHashes = Collections.unmodifiableMap(new LinkedHashMap<>(fileHashes));
        cachedServerVersion = serverVersion;
        cachedServerType = serverType;
        return updates;
    }

    private static List<PluginUpdate> checkProvider(
        UpdateSourceProvider provider,
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        Map<String, String> fileHashes,
        String serverVersion,
        ServerType serverType,
        PluginUpdateConfig config
    ) {
        try {
            List<PluginUpdate> updates = provider.check(
                pluginsPath,
                plugins,
                fileHashes,
                serverVersion,
                serverType,
                config
            );
            return updates == null ? List.of() : updates;
        } catch (IOException | RuntimeException e) {
            // A single failed source must never abort the whole update pass:
            // other providers may still resolve updates for the same plugins.
            LOGGER.log(Level.WARNING, "Update source [" + provider.getSource() + "] failed; skipping it", e);
            return List.of();
        }
    }

    public synchronized void invalidateCache() {
        lastCheckedAt = 0;
        cachedUpdates = Collections.emptyList();
        cachedFileHashes = Collections.emptyMap();
        cachedServerVersion = null;
        cachedServerType = null;
    }

    public synchronized void update(OPanelServer server, List<PluginUpdate> updates) throws IOException, ActLaterException {
        List<Path> tempFiles = new ArrayList<>();
        try {
            for(PluginUpdate update : updates) {
                Path currentFile = resolvePluginFilePath(server.getPluginsPath(), update.getFileName());
                if(
                    currentFile == null
                    || !update.getInstalledFileSha1().equalsIgnoreCase(Utils.sha1(currentFile))
                ) {
                    throw new PluginUpdateConflictException(update.getFileName());
                }
            }

            for(PluginUpdate update : updates) {
                Path tempFile = OPanel.TMP_DIR_PATH.resolve("plugin-update-"+ java.util.UUID.randomUUID() +".jar");
                download(update.getDownloadUrl(), tempFile);

                final String expectedSha1 = update.getFileSha1();
                if(expectedSha1 != null && !expectedSha1.equalsIgnoreCase(Utils.sha1(tempFile))) {
                    throw new IOException("Downloaded file integrity check failed for "+ update.getName());
                }
                tempFiles.add(tempFile);
            }

            final boolean deferred = applyDownloadedUpdates(updates, tempFiles, server::updatePlugin);
            if(deferred) {
                throw new ActLaterException();
            }
        } finally {
            invalidateCache();
            for(Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    public Map<String, PluginUpdateBinding> getBindingsSnapshot() {
        return configStore.getBindingsSnapshot();
    }

    public PluginUpdateBinding getBinding(String fileName) {
        return configStore.getBinding(fileName);
    }

    public void setBinding(PluginUpdateBinding binding) {
        configStore.setBinding(binding);
    }

    public void removeBinding(String fileName) {
        configStore.removeBinding(fileName);
    }

    public long getLastCheckedAt() {
        return lastCheckedAt;
    }

    public List<PluginUpdate> getCachedUpdates() {
        return new ArrayList<>(cachedUpdates);
    }

    private static boolean applyDownloadedUpdates(
        List<PluginUpdate> updates,
        List<Path> tempFiles,
        PluginFileUpdater updater
    ) throws IOException {
        boolean deferred = false;
        for(int i = 0; i < updates.size(); i++) {
            try {
                updater.update(updates.get(i).getFileName(), tempFiles.get(i));
            } catch (ActLaterException e) {
                deferred = true;
            }
        }
        return deferred;
    }

    @FunctionalInterface
    private interface PluginFileUpdater {
        void update(String fileName, Path newPluginFile) throws IOException, ActLaterException;
    }

    private static Map<String, String> calculateFileHashes(Path pluginsPath, List<OPanelPlugin> plugins) {
        Map<String, String> fileHashes = new LinkedHashMap<>();
        for(OPanelPlugin plugin : plugins) {
            Path filePath = resolvePluginFilePath(pluginsPath, plugin.getFileName());
            if(filePath == null) continue;
            try {
                fileHashes.put(plugin.getFileName(), Utils.sha1(filePath));
            } catch (IOException e) {
                // Skip files that cannot be read.
            }
        }
        return fileHashes;
    }

    private static Path resolvePluginFilePath(Path pluginsPath, String fileName) {
        Path path = pluginsPath.resolve(fileName);
        if(Files.exists(path)) return path;
        Path disabledPath = pluginsPath.resolve(fileName + OPanelPlugin.DISABLED_SUFFIX);
        return Files.exists(disabledPath) ? disabledPath : null;
    }

    private static void download(String url, Path target) throws IOException {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(10))
            .header("User-Agent", "OPanel/"+ OPanel.VERSION +" (https://github.com/opanel-mc/opanel)")
            .GET()
            .build();
        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if(response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IOException("Failed to download the update file (HTTP " + response.statusCode() +")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(target);
            throw new IOException("Interrupted while downloading the update file", e);
        }
    }
}
