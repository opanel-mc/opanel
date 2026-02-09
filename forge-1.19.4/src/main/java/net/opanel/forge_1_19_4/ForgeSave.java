package net.opanel.forge_1_19_4;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.opanel.common.OPanelDifficulty;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.forge_helper.BaseForgeSave;
import net.opanel.forge_helper.utils.ForgeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;

public class ForgeSave extends BaseForgeSave implements OPanelSave {
    private CompoundTag nbt;

    public ForgeSave(MinecraftServer server, Path path) {
        super(server, path);

        try {
            nbt = NbtIo.readCompressed(savePath.resolve("level.dat").toFile())
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
        NbtIo.writeCompressed(dataNbt, savePath.resolve("level.dat").toFile());
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
        int gamemode = nbt.getInt("GameType");
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

        byte difficulty = nbt.getByte("Difficulty");
        return OPanelDifficulty.fromId(difficulty);
    }

    @Override
    public void setDifficulty(OPanelDifficulty difficulty) throws IOException {
        if(isCurrent()) server.setDifficulty(Difficulty.byName(difficulty.getName()), true);

        nbt.putByte("Difficulty", (byte) difficulty.getId());
        saveNbt();
    }

    @Override
    public boolean isDifficultyLocked() throws IOException {
        if(isCurrent()) return getCurrentWorld().getLevelData().isDifficultyLocked();

        return nbt.getByte("DifficultyLocked") == 1;
    }

    @Override
    public void setDifficultyLocked(boolean locked) throws IOException {
        if(isCurrent()) server.setDifficultyLocked(locked);

        nbt.putByte("DifficultyLocked", (byte) (locked ? 1 : 0));
        saveNbt();
    }

    @Override
    public boolean isHardcore() throws IOException {
        if(isCurrent()) return server.isHardcore();

        return nbt.getByte("hardcore") == 1;
    }

    @Override
    public void setHardcoreEnabled(boolean enabled) throws IOException {
        if(isCurrent()) {
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
            OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("hardcore=.+", "hardcore="+ enabled));
            ForgeUtils.forceUpdateProperties((DedicatedServer) server, true);
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

        if(isCurrent()) {
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
}
