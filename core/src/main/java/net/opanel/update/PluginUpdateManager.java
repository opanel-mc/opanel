package net.opanel.update;

import net.opanel.common.OPanelPlugin;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;
import net.opanel.exception.ActLaterException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade kept for existing call sites. The coordinator isolates failures from
 * individual providers so one unavailable source cannot abort the whole pass.
 */
public class PluginUpdateManager {
    private final PluginUpdateCoordinator coordinator;

    public PluginUpdateManager(long checkIntervalSeconds) {
        this(checkIntervalSeconds, "");
    }

    public PluginUpdateManager(long checkIntervalSeconds, String curseForgeApiKey) {
        this(buildProviders(curseForgeApiKey), checkIntervalSeconds);
    }

    private static List<UpdateSourceProvider> buildProviders(String curseForgeApiKey) {
        List<UpdateSourceProvider> providers = new ArrayList<>();
        providers.add(new ModrinthUpdateSourceProvider());
        providers.add(new GitHubReleaseUpdateSourceProvider());
        providers.add(new HangarUpdateSourceProvider());
        providers.add(new CurseForgeUpdateSourceProvider(curseForgeApiKey));
        return providers;
    }

    public PluginUpdateManager(List<UpdateSourceProvider> providers, long checkIntervalSeconds) {
        this.coordinator = new PluginUpdateCoordinator(providers, checkIntervalSeconds);
    }

    public List<PluginUpdate> check(
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        String serverVersion,
        ServerType serverType,
        boolean force
    ) throws IOException {
        return coordinator.check(pluginsPath, plugins, serverVersion, serverType, force);
    }

    public void update(OPanelServer server, List<PluginUpdate> updates) throws IOException, ActLaterException {
        coordinator.update(server, updates);
    }

    public void invalidateCache() {
        coordinator.invalidateCache();
    }

    public PluginUpdateCoordinator getCoordinator() {
        return coordinator;
    }
}
