package net.opanel.forge_helper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public abstract class BaseForgeSave implements OPanelSave {
    protected static final long NBT_TRACKER_SIZE = 2097152; // 2 MB

    protected final MinecraftServer server;
    protected final Path savePath;

    public BaseForgeSave(MinecraftServer server, Path path) {
        this.server = server;
        savePath = path;
    }

    protected abstract void saveNbt() throws IOException;

    protected ServerLevel getCurrentWorld() {
        return server.overworld();
    }

    @Override
    public String getName() {
        return savePath.getFileName().toString();
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
    public void delete() throws IOException {
        Utils.deleteDirectoryRecursively(savePath);
    }
}
