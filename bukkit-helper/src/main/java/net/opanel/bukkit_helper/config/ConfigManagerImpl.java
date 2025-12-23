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
