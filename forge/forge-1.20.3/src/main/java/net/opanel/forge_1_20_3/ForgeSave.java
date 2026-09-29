package net.opanel.forge_1_20_3;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.opanel.common.*;
import net.opanel.forge_helper.BaseForgeSave;
import net.opanel.forge_helper.utils.ForgeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class ForgeSave extends BaseForgeSave implements OPanelSave {
    private CompoundTag nbt;

    public ForgeSave(MinecraftServer server, Path path) {
        super(server, path);

        try {
            nbt = NbtIo.readCompressed(savePath.resolve("level.dat"), NbtAccounter.unlimitedHeap())
                    .getCompound("Data");
            if(nbt.isEmpty()) {
                throw new IOException("Cannot find a valid level.dat");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveNbt() throws IOException {
        CompoundTag dataNbt = new CompoundTag();
        dataNbt.put("Data", nbt);
        NbtIo.writeCompressed(dataNbt, savePath.resolve("level.dat"));
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
    public OPanelGameMode getDefaultGameMode() {
        if(isRunning()) return OPanelGameMode.fromId(server.getDefaultGameType().getId());

        int gamemode = nbt.getInt("GameType");
        return OPanelGameMode.fromId(gamemode);
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {
        if(isRunning()) server.setDefaultGameType(GameType.byId(gamemode.getId()));

        nbt.putInt("GameType", gamemode.getId());
        saveNbt();
    }

    @Override
    public OPanelDifficulty getDifficulty() throws IOException {
        if(isRunning()) return OPanelDifficulty.fromId(getCurrentWorld().getDifficulty().getId());

        byte difficulty = nbt.getByte("Difficulty");
        return OPanelDifficulty.fromId(difficulty);
    }

    @Override
    public void setDifficulty(OPanelDifficulty difficulty) throws IOException {
        if(isRunning()) server.setDifficulty(Difficulty.byName(difficulty.getName()), true);

        nbt.putByte("Difficulty", (byte) difficulty.getId());
        saveNbt();
    }

    @Override
    public boolean isDifficultyLocked() throws IOException {
        if(isRunning()) return getCurrentWorld().getLevelData().isDifficultyLocked();

        return nbt.getByte("DifficultyLocked") == 1;
    }

    @Override
    public void setDifficultyLocked(boolean locked) throws IOException {
        if(isRunning()) server.setDifficultyLocked(locked);

        nbt.putByte("DifficultyLocked", (byte) (locked ? 1 : 0));
        saveNbt();
    }

    @Override
    public boolean isHardcore() throws IOException {
        if(isRunning()) return server.isHardcore();

        return nbt.getByte("hardcore") == 1;
    }

    @Override
    public void setHardcoreEnabled(boolean enabled) throws IOException {
        if(isRunning()) {
            PrimaryLevelData worldData = (PrimaryLevelData) getCurrentWorld().getLevelData();
            LevelSettings currentSettings = worldData.getLevelSettings();
            LevelSettings newSettings = new LevelSettings(
                    currentSettings.levelName(),
                    currentSettings.gameType(),
                    enabled,
                    currentSettings.difficulty(),
                    currentSettings.allowCommands(),
                    currentSettings.gameRules(),
                    currentSettings.getDataConfiguration()
            );
            try {
                Field settingsField = PrimaryLevelData.class.getDeclaredField("f_78443_"); // f_78443_ -> settings
                settingsField.setAccessible(true);
                settingsField.set(worldData, newSettings);
            } catch (ReflectiveOperationException e) {
                //
            }
            ForgeUtils.setRuntimeHardcore((DedicatedServer) server, enabled, true);
        }

        // Persist the startup setting only for the save selected in server.properties.
        if(isCurrent()) {
            OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("hardcore=.+", "hardcore="+ enabled));
        }

        nbt.putByte("hardcore", (byte) (enabled ? 1 : 0));
        saveNbt();
    }

    @Override
    public HashMap<String, Boolean> getDatapacks() {
        HashMap<String, Boolean> datapacks = new HashMap<>();
        CompoundTag datapacksNbt = nbt.getCompound("DataPacks");
        datapacksNbt.getList("Enabled", Tag.TAG_STRING).forEach(tag -> datapacks.put(tag.getAsString(), true));
        datapacksNbt.getList("Disabled", Tag.TAG_STRING).forEach(tag -> datapacks.put(tag.getAsString(), false));
        return datapacks;
    }

    @Override
    public void toggleDatapack(String id, boolean enabled) throws IOException {
        Boolean currentEnabled = getDatapacks().get(id);
        if(currentEnabled == null || currentEnabled == enabled) return;
        if(id.equals("vanilla")) return;

        if(isRunning()) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "datapack "+ (enabled ? "enable" : "disable") +" \""+ id +"\"");
        }

        CompoundTag datapacksNbt = nbt.getCompound("DataPacks");
        if(enabled) {
            datapacksNbt.getList("Disabled", Tag.TAG_STRING).remove(StringTag.valueOf(id));
            datapacksNbt.getList("Enabled", Tag.TAG_STRING).add(StringTag.valueOf(id));
        } else {
            datapacksNbt.getList("Enabled", Tag.TAG_STRING).remove(StringTag.valueOf(id));
            datapacksNbt.getList("Disabled", Tag.TAG_STRING).add(StringTag.valueOf(id));
        }
        saveNbt();
    }

    @Override
    public List<OPanelWorldRegion> getRegions() {
        List<OPanelWorldRegion> regions = new ArrayList<>();

        Path regionFolderPath = savePath.resolve("region");
        if(!regionFolderPath.toFile().exists()) return regions;

        try(Stream<Path> stream = Files.list(regionFolderPath)) {
            stream.filter(path -> (
                            path.toString().endsWith(".mca")
                                    && path.toFile().isFile()
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        ForgeWorldRegion region = new ForgeWorldRegion(server, savePath.getFileName().toString(), path);
                        regions.add(region);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }
}
