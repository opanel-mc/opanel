package net.opanel.neoforge_1_21_5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class NeoSave implements OPanelSave {
    private final MinecraftServer server;
    private final Path savePath;
    private CompoundTag nbt;

    public NeoSave(MinecraftServer server, Path path) {
        this.server = server;
        savePath = path;
        try {
            Optional<CompoundTag> optionalNbt = NbtIo.readCompressed(savePath.resolve("level.dat"), NbtAccounter.create(2097152L)) // 2 MB
                    .get("Data").asCompound();
            if(optionalNbt.isEmpty()) {
                throw new IOException("Cannot find a valid level.dat");
            }
            nbt = optionalNbt.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNbt() throws IOException {
        CompoundTag dataNbt = new CompoundTag();
        dataNbt.put("Data", nbt);
        NbtIo.writeCompressed(dataNbt, savePath.resolve("level.dat"));
    }

    @Override
    public String getName() {
        return savePath.getFileName().toString();
    }

    @Override
    public String getDisplayName() {
        return nbt.getStringOr("LevelName", "world").replaceAll("\u00C2", "");
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
        return server.getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent().getFileName().toString().equals(getName());
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
        GameType gamemode = GameType.byId(nbt.getIntOr("GameType", 0));
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
