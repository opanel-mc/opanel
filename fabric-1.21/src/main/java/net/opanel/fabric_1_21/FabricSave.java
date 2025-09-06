package net.opanel.fabric_1_21;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;
import org.intellij.lang.annotations.RegExp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

public class FabricSave implements OPanelSave {
    private final MinecraftServer server;
    private final Path savePath;
    private NbtCompound nbt;

    public FabricSave(MinecraftServer server, Path path) {
        this.server = server;
        savePath = path;
        try {
            nbt = NbtIo.readCompressed(savePath.resolve("level.dat"), NbtSizeTracker.of(2097152L)) // 2 MB
                    .getCompound("Data");
            if(nbt.isEmpty()) {
                throw new IOException("Cannot find a valid level.dat");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNbt() throws IOException {
        NbtCompound dataNbt = new NbtCompound();
        dataNbt.put("Data", nbt);
        NbtIo.writeCompressed(dataNbt, savePath.resolve("level.dat"));
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
        nbt.putString("LevelName", displayName);
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
        return server.getSavePath(WorldSavePath.LEVEL_DAT).getParent().getFileName().toString().equals(getName());
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
        GameMode gamemode = GameMode.byId(nbt.getInt("GameType"));
        switch(gamemode) {
            case ADVENTURE -> { return OPanelGameMode.ADVENTURE; }
            case SURVIVAL -> { return OPanelGameMode.SURVIVAL; }
            case CREATIVE -> { return OPanelGameMode.CREATIVE; }
            case SPECTATOR -> { return OPanelGameMode.SPECTATOR; }
        }
        return null;
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {
        switch(gamemode) {
            case ADVENTURE -> nbt.putInt("GameType", 2);
            case SURVIVAL -> nbt.putInt("GameType", 0);
            case CREATIVE -> nbt.putInt("GameType", 1);
            case SPECTATOR -> nbt.putInt("GameType", 3);
        }
        saveNbt();
    }

    @Override
    public void delete() throws IOException {
        Utils.deleteDirectoryRecursively(savePath);
    }
}
