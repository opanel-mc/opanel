package net.opanel.fabric_26_1;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.opanel.common.*;
import net.opanel.fabric_helper_unmapped.BaseFabricSave;
import net.opanel.fabric_helper_unmapped.utils.FabricUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FabricSave extends BaseFabricSave implements OPanelSave {
    private CompoundTag nbt;
    private CompoundTag difficultySettingsNbt;

    public FabricSave(MinecraftServer server, Path path) {
        super(server, path);

        try {
            Optional<CompoundTag> optionalNbt = NbtIo.readCompressed(savePath.resolve("level.dat"), NbtAccounter.unlimitedHeap())
                    .get("Data").asCompound();
            if(optionalNbt.isEmpty()) {
                throw new IOException("Cannot find a valid level.dat");
            }
            nbt = optionalNbt.get();

            Optional<CompoundTag> optionalDifficultySettingsNbt = nbt.getCompound("difficulty_settings");
            if(optionalDifficultySettingsNbt.isEmpty()) {
                throw new IOException("Cannot read difficulty_settings");
            }
            difficultySettingsNbt = optionalDifficultySettingsNbt.get();
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
    protected void saveDifficultySettings() throws IOException {
        nbt.put("difficulty_settings", difficultySettingsNbt);
        saveNbt();
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
    public OPanelGameMode getDefaultGameMode() {
        int gamemode = nbt.getIntOr("GameType", 0);
        return OPanelGameMode.fromId(gamemode);
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {
        nbt.putInt("GameType", gamemode.getId());
        saveNbt();
    }

    @Override
    public OPanelDifficulty getDifficulty() throws IOException {
        if(isCurrent()) return OPanelDifficulty.fromId(getCurrentWorld().getDifficulty().getId());

        String difficulty = difficultySettingsNbt.getStringOr("difficulty", "easy");
        return OPanelDifficulty.fromString(difficulty);
    }

    @Override
    public void setDifficulty(OPanelDifficulty difficulty) throws IOException {
        if(isCurrent()) server.setDifficulty(Difficulty.byName(difficulty.getName()), true);

        difficultySettingsNbt.putString("difficulty", difficulty.getName());
        saveDifficultySettings();
    }

    @Override
    public boolean isDifficultyLocked() throws IOException {
        if(isCurrent()) return getCurrentWorld().getLevelData().isDifficultyLocked();

        return difficultySettingsNbt.getByteOr("locked", (byte) 0) == 1;
    }

    @Override
    public void setDifficultyLocked(boolean locked) throws IOException {
        if(isCurrent()) server.setDifficultyLocked(locked);

        difficultySettingsNbt.putByte("locked", (byte) (locked ? 1 : 0));
        saveDifficultySettings();
    }

    @Override
    public boolean isHardcore() throws IOException {
        if(isCurrent()) return server.isHardcore();

        return difficultySettingsNbt.getByteOr("hardcore", (byte) 0) == 1;
    }

    @Override
    public void setHardcoreEnabled(boolean enabled) throws IOException {
        if(isCurrent()) {
            PrimaryLevelData worldData = (PrimaryLevelData) getCurrentWorld().getLevelData();
            LevelSettings currentSettings = worldData.getLevelSettings();
            LevelSettings.DifficultySettings currentDifficulty = currentSettings.difficultySettings();
            LevelSettings newSettings = new LevelSettings(
                    currentSettings.levelName(),
                    currentSettings.gameType(),
                    new LevelSettings.DifficultySettings(
                            currentDifficulty.difficulty(),
                            enabled,
                            currentDifficulty.locked()
                    ),
                    currentSettings.allowCommands(),
                    currentSettings.dataConfiguration()
            );
            try {
                Field settingsField = PrimaryLevelData.class.getDeclaredField("settings");
                settingsField.setAccessible(true);
                settingsField.set(worldData, newSettings);
            } catch (ReflectiveOperationException e) {
                //
            }
            OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("hardcore=.+", "hardcore="+ enabled));
            FabricUtils.forceUpdateProperties((DedicatedServer) server);
        }

        difficultySettingsNbt.putByte("hardcore", (byte) (enabled ? 1 : 0));
        saveDifficultySettings();
    }

    @Override
    public HashMap<String, Boolean> getDatapacks() {
        HashMap<String, Boolean> datapacks = new HashMap<>();

        Optional<CompoundTag> optionalDatapacksNbt = nbt.getCompound("DataPacks");
        if(optionalDatapacksNbt.isEmpty()) return datapacks;
        CompoundTag datapacksNbt = optionalDatapacksNbt.get();

        Optional<ListTag> optionalEnabledListNbt = datapacksNbt.getList("Enabled");
        Optional<ListTag> optionalDisabledListNbt = datapacksNbt.getList("Disabled");

        optionalEnabledListNbt.ifPresent(tags -> tags.forEach(tag -> datapacks.put(tag.asString().get(), true)));
        optionalDisabledListNbt.ifPresent(tags -> tags.forEach(tag -> datapacks.put(tag.asString().get(), false)));
        return datapacks;
    }

    @Override
    public void toggleDatapack(String id, boolean enabled) throws IOException {
        Boolean currentEnabled = getDatapacks().get(id);
        if(currentEnabled == null || currentEnabled == enabled) return;
        if(id.equals("vanilla")) return;

        if(isCurrent()) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "datapack "+ (enabled ? "enable" : "disable") +" \""+ id +"\"");
        }

        Optional<CompoundTag> optionalDatapacksNbt = nbt.getCompound("DataPacks");
        if(optionalDatapacksNbt.isEmpty()) return;
        CompoundTag datapacksNbt = optionalDatapacksNbt.get();

        Optional<ListTag> optionalEnabledListNbt = datapacksNbt.getList("Enabled");
        Optional<ListTag> optionalDisabledListNbt = datapacksNbt.getList("Disabled");

        if(enabled) {
            optionalDisabledListNbt.ifPresent(tags -> tags.remove(StringTag.valueOf(id)));
            optionalEnabledListNbt.ifPresent(tags -> tags.add(StringTag.valueOf(id)));
        } else {
            optionalEnabledListNbt.ifPresent(tags -> tags.remove(StringTag.valueOf(id)));
            optionalDisabledListNbt.ifPresent(tags -> tags.add(StringTag.valueOf(id)));
        }
        saveNbt();
    }

    @Override
    public List<OPanelWorldRegion> getRegions() {
        List<OPanelWorldRegion> regions = new ArrayList<>();

        Path regionFolderPath = savePath.resolve("dimensions/minecraft/overworld/region");
        if(!regionFolderPath.toFile().exists()) return regions;

        try(Stream<Path> stream = Files.list(regionFolderPath)) {
            stream.filter(path -> (
                            path.toString().endsWith(".mca")
                                    && path.toFile().isFile()
                    ))
                    .map(Path::toAbsolutePath)
                    .forEach(path -> {
                        FabricWorldRegion region = new FabricWorldRegion(server, savePath.getFileName().toString(), path);
                        regions.add(region);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }
}
