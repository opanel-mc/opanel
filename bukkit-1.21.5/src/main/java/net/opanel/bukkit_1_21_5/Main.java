package net.opanel.bukkit_1_21_5;

import net.opanel.OPanel;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public OPanel instance;

    @Override
    public void onEnable() {
        LoggerImpl logger = new LoggerImpl(this.getLogger());
    }

    @Override
    public void onDisable() {

    }
}