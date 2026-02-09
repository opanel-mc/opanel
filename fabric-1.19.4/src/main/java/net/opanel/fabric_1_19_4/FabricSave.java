package net.opanel.fabric_1_19_4;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.opanel.common.OPanelDifficulty;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.fabric_helper.BaseFabricSave;
import net.opanel.fabric_helper.utils.FabricUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;

public class FabricSave extends BaseFabricSave implements OPanelSave {
    private NbtCompound nbt;

    public FabricSave(MinecraftServer server, Path path) {
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
        NbtCompound dataNbt = new NbtCompound();
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
        if(isCurrent()) return getCurrentWorld().getLevelProperties().isDifficultyLocked();

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
            LevelProperties levelProperties = (LevelProperties) getCurrentWorld().getLevelProperties();
            LevelInfo currentInfo = levelProperties.getLevelInfo();
            LevelInfo newInfo = new LevelInfo(
                    currentInfo.getLevelName(),
                    currentInfo.getGameMode(),
                    enabled,
                    currentInfo.getDifficulty(),
                    currentInfo.areCommandsAllowed(),
                    currentInfo.getGameRules(),
                    currentInfo.getDataConfiguration()
            );
            try {
                Field levelInfoField = LevelProperties.class.getDeclaredField("levelInfo");
                levelInfoField.setAccessible(true);
                levelInfoField.set(levelProperties, newInfo);
            } catch (ReflectiveOperationException e) {
                //
            }
            OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("hardcore=.+", "hardcore="+ enabled));
            FabricUtils.forceUpdateProperties((MinecraftDedicatedServer) server);
        }

        nbt.putByte("hardcore", (byte) (enabled ? 1 : 0));
        saveNbt();
    }

    @Override
    public HashMap<String, Boolean> getDatapacks() {
        HashMap<String, Boolean> datapacks = new HashMap<>();
        NbtCompound datapacksNbt = nbt.getCompound("DataPacks");
        datapacksNbt.getList("Enabled", NbtElement.STRING_TYPE).forEach(elem -> datapacks.put(elem.asString(), true));
        datapacksNbt.getList("Disabled", NbtElement.STRING_TYPE).forEach(elem -> datapacks.put(elem.asString(), false));
        return datapacks;
    }

    @Override
    public void toggleDatapack(String id, boolean enabled) throws IOException {
        Boolean currentEnabled = getDatapacks().get(id);
        if(currentEnabled == null || currentEnabled == enabled) return;
        if(id.equals("vanilla")) return;

        if(isCurrent()) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "datapack "+ (enabled ? "enable" : "disable") +" \""+ id +"\"");
        }

        NbtCompound datapacksNbt = nbt.getCompound("DataPacks");
        if(enabled) {
            datapacksNbt.getList("Disabled", NbtElement.STRING_TYPE).remove(NbtString.of(id));
            datapacksNbt.getList("Enabled", NbtElement.STRING_TYPE).add(NbtString.of(id));
        } else {
            datapacksNbt.getList("Enabled", NbtElement.STRING_TYPE).remove(NbtString.of(id));
            datapacksNbt.getList("Disabled", NbtElement.STRING_TYPE).add(NbtString.of(id));
        }
        saveNbt();
    }
}
