package net.opanel.bukkit_1_21_5;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Server;

import java.io.IOException;
import java.nio.file.Path;

public class BukkitSave implements OPanelSave {
    private final Server server;
    private final Path savePath;
    private ReadWriteNBT nbt;

    public BukkitSave(Server server, Path path) {
        this.server = server;
        savePath = path;
        try {
            nbt = NBT.readFile(savePath.resolve("level.dat").toFile()).getCompound("Data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNbt() throws IOException {
        NBT.writeFile(savePath.resolve("level.dat").toFile(), nbt);
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
    public boolean isCurrent() {
        return server.getWorlds().getFirst().getName().equals(getName());
    }

    @Override
    public OPanelGameMode getDefaultGameMode() {
        GameMode gamemode = GameMode.getByValue(nbt.getInteger("GameType"));
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
