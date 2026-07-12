package net.opanel.bukkit_helper.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManagerImpl implements ConfigManager {
    private final FileConfiguration configSrc;
    private final JavaPlugin plugin;

    public ConfigManagerImpl(FileConfiguration configSrc, JavaPlugin plugin) {
        this.configSrc = configSrc;
        this.plugin = plugin;
    }

    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                configSrc.getString("accessKey"),
                configSrc.getString("salt"),
                configSrc.getString("webServerHost", OPanelConfiguration.defaultConfig.webServerHost),
                configSrc.getInt("webServerPort", OPanelConfiguration.defaultConfig.webServerPort),
                configSrc.getInt("mcdrSocketPort"),
                configSrc.getInt("mapPrerenderConcurrent", OPanelConfiguration.defaultConfig.mapPrerenderConcurrent),
                configSrc.getInt("monitorSnapshotInterval", OPanelConfiguration.defaultConfig.monitorSnapshotInterval),
                configSrc.getBoolean("cookieSecure", OPanelConfiguration.defaultConfig.cookieSecure),
                configSrc.getBoolean("proxyHeaders", OPanelConfiguration.defaultConfig.proxyHeaders),
                configSrc.getBoolean("oidcEnabled", OPanelConfiguration.defaultConfig.oidcEnabled),
                configSrc.getString("oidcDiscoveryUrl"),
                configSrc.getString("oidcClientId"),
                configSrc.getString("oidcClientSecret"),
                configSrc.getString("oidcDisplayName")
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set("accessKey", config.accessKey);
        configSrc.set("salt", config.salt);
        configSrc.set("webServerHost", config.webServerHost);
        configSrc.set("webServerPort", config.webServerPort);
        configSrc.set("mcdrSocketPort", config.mcdrSocketPort);
        configSrc.set("mapPrerenderConcurrent", config.mapPrerenderConcurrent);
        configSrc.set("monitorSnapshotInterval", config.monitorSnapshotInterval);
        configSrc.set("cookieSecure", config.cookieSecure);
        configSrc.set("proxyHeaders", config.proxyHeaders);
        configSrc.set("oidcEnabled", config.oidcEnabled);
        configSrc.set("oidcDiscoveryUrl", config.oidcDiscoveryUrl);
        configSrc.set("oidcClientId", config.oidcClientId);
        configSrc.set("oidcClientSecret", config.oidcClientSecret);
        configSrc.set("oidcDisplayName", config.oidcDisplayName);
        plugin.saveConfig();
    }
}
