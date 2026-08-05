package net.opanel.update;

import net.opanel.OPanel;
import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.exception.ActLaterException;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PluginAutoUpdateService {
    private final OPanel plugin;
    private final ScheduledExecutorService executor;
    private volatile boolean started;

    public PluginAutoUpdateService(OPanel plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "opanel-plugin-auto-update");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if(started) return;
        started = true;
        final long intervalSeconds = Math.max(plugin.getConfig().pluginUpdateCheckInterval, 60);
        executor.scheduleWithFixedDelay(this::run, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void run() {
        try {
            if(!plugin.getConfig().autoCheckPluginUpdates) return;

            final OPanelServer server = plugin.getServer();
            if(server == null) return;

            final java.util.Set<String> pending = plugin.getPendingPluginOperations();
            final List<OPanelPlugin> eligiblePlugins = server.getPlugins().stream()
                .filter(item -> !pending.contains(item.getFileName()))
                .collect(Collectors.toList());

            final PluginUpdateManager manager = plugin.getPluginUpdateManager();
            final List<PluginUpdate> updates = manager.check(
                server.getPluginsPath(),
                eligiblePlugins,
                server.getVersion(),
                server.getServerType(),
                false
            );
            if(!plugin.getConfig().autoApplyPluginUpdates) return;

            final List<PluginUpdate> safeUpdates = updates.stream()
                .filter(PluginUpdate::isAutoApplySafe)
                .collect(Collectors.toList());
            if(!safeUpdates.isEmpty()) {
                manager.update(server, safeUpdates);
            }
        } catch (ActLaterException e) {
            // The update was staged and will be applied on the next restart.
        } catch (Exception e) {
            plugin.logger.warn("Background plugin update check failed: " + e.getMessage());
        }
    }

    public void shutdown() {
        started = false;
        executor.shutdownNow();
    }
}
