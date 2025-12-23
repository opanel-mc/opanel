package net.opanel.bukkit_helper;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelDifficulty;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import org.bukkit.Difficulty;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

public abstract class BaseBukkitSave implements OPanelSave {
    protected final JavaPlugin plugin;
    protected final TaskRunner runner;
    protected final Server server;
    protected final Path savePath;
    protected ReadWriteNBT nbt;

    public BaseBukkitSave(JavaPlugin plugin, Server server, Path path) {
        this.plugin = plugin;
        runner = (TaskRunner) plugin;
        this.server = server;
        savePath = path;
        try {
            nbt = NBT.readFile(savePath.resolve("level.dat").toFile()).getCompound("Data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void saveNbt() throws IOException {
        ReadWriteNBT dataNbt = NBT.createNBTObject();
        dataNbt.set("Data", nbt, NBTHandlers.STORE_READWRITE_TAG);
        NBT.writeFile(savePath.resolve("level.dat").toFile(), dataNbt);
    }

    protected World getCurrentWorld() {
        return server.getWorlds().get(0);
    }

    @Override
    public String getName() {
        return savePath.getFileName().toString();
    }

    @Override
    public String getDisplayName() {
        return nbt.getString("LevelName").replaceAll("\u00C2", "");
    }

    @Override
    public void setDisplayName(String displayName) throws IOException {
        nbt.setString("LevelName", displayName);
        saveNbt();
    }

    @Override
    public Path getPath() {
        return savePath.toAbsolutePath();
    }

    @Override
    public long getSize() throws IOException {
        return Utils.getDirectorySize(savePath);
    }

    @Override
    public boolean isRunning() {
        return getCurrentWorld().getName().equals(getName());
    }

    @Override
    public boolean isCurrent() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(OPanelServer.serverPropertiesPath.toFile()));
        return properties.getProperty("level-name").replaceAll("\u00c2", "").equals(getName());
    }

    @Override
    public void setToCurrent() throws IOException {
        if(isCurrent()) return;
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("level-name=.+", "level-name="+ getName()));
    }

    @Override
    public OPanelGameMode getDefaultGameMode() {
        int gamemode = nbt.getInteger("GameType");
        return OPanelGameMode.fromId(gamemode);
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {
        nbt.setInteger("GameType", gamemode.getId());
        saveNbt();
    }

    @Override
    public OPanelDifficulty getDifficulty() throws IOException {
        if(isCurrent()) return OPanelDifficulty.fromId(getCurrentWorld().getDifficulty().getValue());

        byte difficulty = nbt.getByte("Difficulty");
        return OPanelDifficulty.fromId(difficulty);
    }

    @Override
    public void setDifficulty(OPanelDifficulty difficulty) throws IOException {
        if(isCurrent()) {
            runner.runTask(() -> {
                switch(difficulty) {
                    case PEACEFUL -> getCurrentWorld().setDifficulty(Difficulty.PEACEFUL);
                    case EASY -> getCurrentWorld().setDifficulty(Difficulty.EASY);
                    case NORMAL -> getCurrentWorld().setDifficulty(Difficulty.NORMAL);
                    case HARD -> getCurrentWorld().setDifficulty(Difficulty.HARD);
                }
            });
        }

        nbt.setByte("Difficulty", (byte) difficulty.getId());
        saveNbt();
    }

    @Override
    public boolean isDifficultyLocked() {
        return nbt.getByte("DifficultyLocked") == 1;
    }

    @Override
    public void setDifficultyLocked(boolean locked) throws IOException {
        nbt.setByte("DifficultyLocked", (byte) (locked ? 1 : 0));
        saveNbt();
    }

    @Override
    public boolean isHardcore() throws IOException {
        if(isCurrent()) return getCurrentWorld().isHardcore();

        return nbt.getByte("hardcore") == 1;
    }

    @Override
    public void setHardcoreEnabled(boolean enabled) throws IOException {
        if(isCurrent()) {
            runner.runTask(() -> getCurrentWorld().setHardcore(enabled));
        }

        nbt.setByte("hardcore", (byte) (enabled ? 1 : 0));
        saveNbt();
    }

    @Override
    public HashMap<String, Boolean> getDatapacks() {
        HashMap<String, Boolean> datapacks = new HashMap<>();
        ReadWriteNBT datapacksNbt = nbt.getCompound("DataPacks");
        datapacksNbt.getStringList("Enabled").forEach(name -> datapacks.put(name, true));
        datapacksNbt.getStringList("Disabled").forEach(name -> datapacks.put(name, false));
        return datapacks;
    }

    @Override
    public void toggleDatapack(String id, boolean enabled) throws IOException {
        Boolean currentEnabled = getDatapacks().get(id);
        if(currentEnabled == null || currentEnabled == enabled) return;
        if(id.equals("vanilla")) return;

        if(isCurrent()) {
            runner.runTask(() -> {
                server.dispatchCommand(server.getConsoleSender(), "datapack "+ (enabled ? "enable" : "disable") +" \""+ id +"\"");
            });
        }

        ReadWriteNBT datapacksNbt = nbt.getCompound("DataPacks");
        if(enabled) {
            datapacksNbt.getStringList("Disabled").remove(id);
            datapacksNbt.getStringList("Enabled").add(id);
        } else {
            datapacksNbt.getStringList("Enabled").remove(id);
            datapacksNbt.getStringList("Disabled").add(id);
        }
        saveNbt();
    }

    @Override
    public void delete() throws IOException {
        Utils.deleteDirectoryRecursively(savePath);
    }
}
