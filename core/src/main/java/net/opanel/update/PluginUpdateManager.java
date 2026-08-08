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
    private final ModrinthUpdateSourceProvider modrinthProvider;

    public PluginUpdateManager(long checkIntervalSeconds) {
        this(checkIntervalSeconds, "", "");
    }

    public PluginUpdateManager(long checkIntervalSeconds, String curseForgeApiKey) {
        this(checkIntervalSeconds, curseForgeApiKey, "");
    }

    public PluginUpdateManager(long checkIntervalSeconds, String curseForgeApiKey, String modrinthSource) {
        this.modrinthProvider = new ModrinthUpdateSourceProvider(modrinthSource);
        List<UpdateSourceProvider> providers = new ArrayList<>();
        providers.add(modrinthProvider);
        providers.add(new GitHubReleaseUpdateSourceProvider());
        providers.add(new HangarUpdateSourceProvider());
        providers.add(new CurseForgeUpdateSourceProvider(curseForgeApiKey));
        this.coordinator = new PluginUpdateCoordinator(providers, checkIntervalSeconds);
    }

    public PluginUpdateManager(List<UpdateSourceProvider> providers, long checkIntervalSeconds) {
        this.coordinator = new PluginUpdateCoordinator(providers, checkIntervalSeconds);
        this.modrinthProvider = null;
    }

    public void setModrinthSource(String source) {
        if(modrinthProvider != null) modrinthProvider.setSource(source);
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
