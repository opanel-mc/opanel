package net.opanel.spigot_1_21;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import org.bukkit.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class SpigotSave implements OPanelSave {
    private final Server server;
    private final Path savePath;
    private ReadWriteNBT nbt;

    public SpigotSave(Server server, Path path) {
        this.server = server;
        savePath = path;
        try {
            nbt = NBT.readFile(savePath.resolve("level.dat").toFile()).getCompound("Data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNbt() throws IOException {
        ReadWriteNBT dataNbt = NBT.createNBTObject();
        dataNbt.set("Data", nbt, NBTHandlers.STORE_READWRITE_TAG);
        NBT.writeFile(savePath.resolve("level.dat").toFile(), dataNbt);
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
        return server.getWorlds().getFirst().getName().equals(getName());
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
        switch(gamemode) {
            case 2 -> { return OPanelGameMode.ADVENTURE; }
            case 0 -> { return OPanelGameMode.SURVIVAL; }
            case 1 -> { return OPanelGameMode.CREATIVE; }
            case 3 -> { return OPanelGameMode.SPECTATOR; }
        }
        return null;
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {
        switch(gamemode) {
            case ADVENTURE -> nbt.setInteger("GameType", 2);
            case SURVIVAL -> nbt.setInteger("GameType", 0);
            case CREATIVE -> nbt.setInteger("GameType", 1);
            case SPECTATOR -> nbt.setInteger("GameType", 3);
        }
        saveNbt();
    }

    @Override
    public void delete() throws IOException {
        Utils.deleteDirectoryRecursively(savePath);
    }
}
