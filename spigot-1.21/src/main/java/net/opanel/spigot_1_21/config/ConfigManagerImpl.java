package net.opanel.spigot_1_21.config;

import net.opanel.config.ConfigManager;
import net.opanel.config.OPanelConfiguration;
import net.opanel.spigot_1_21.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManagerImpl implements ConfigManager {
    private final FileConfiguration configSrc;
    private final Main plugin;

    public ConfigManagerImpl(FileConfiguration configSrc, Main plugin) {
        this.configSrc = configSrc;
        this.plugin = plugin;
    }

    @Override
    public OPanelConfiguration get() {
        return new OPanelConfiguration(
                configSrc.getString("accessKey"),
                configSrc.getString("salt"),
                configSrc.getInt("webServerPort")
        );
    }

    @Override
    public void set(OPanelConfiguration config) {
        configSrc.set("accessKey", config.accessKey);
        configSrc.set("salt", config.salt);
        configSrc.set("webServerPort", config.webServerPort);
        plugin.saveConfig();
    }
}
