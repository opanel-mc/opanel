package net.opanel.folia_26_1_2;

import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.opanel.annotation.Rewrite;
import net.opanel.bukkit_helper.BaseBukkitSave;
import net.opanel.common.OPanelDifficulty;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelWorldRegion;
import org.bukkit.Difficulty;
import org.bukkit.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FoliaSave extends BaseBukkitSave implements OPanelSave {
    protected ReadWriteNBT difficultySettingsNbt;

    public FoliaSave(Main plugin, Server server, Path path) {
        super(plugin, server, path);

        difficultySettingsNbt = nbt.getCompound("difficulty_settings");
    }

    private void saveDifficultySettings() throws IOException {
        nbt.set("difficulty_settings", difficultySettingsNbt, NBTHandlers.STORE_READWRITE_TAG);
        saveNbt();
    }

    @Rewrite
    @Override
    public Path getNetherPath() {
        return Path.of("dimensions/minecraft/the_nether");
    }

    @Rewrite
    @Override
    public Path getTheEndPath() {
        return Path.of("dimensions/minecraft/the_end");
    }

    @Rewrite
    @Override
    public OPanelDifficulty getDifficulty() throws IOException {
        if(isCurrent()) return OPanelDifficulty.fromId(getCurrentWorld().getDifficulty().getValue());

        String difficulty = difficultySettingsNbt.getString("difficulty");
        return OPanelDifficulty.fromString(difficulty);
    }

    @Rewrite
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

        difficultySettingsNbt.setString("difficulty", difficulty.getName());
        saveDifficultySettings();
    }

    @Rewrite
    @Override
    public boolean isDifficultyLocked() {
        return difficultySettingsNbt.getByte("locked") == 1;
    }

    @Rewrite
    @Override
    public void setDifficultyLocked(boolean locked) throws IOException {
        difficultySettingsNbt.setByte("locked", (byte) (locked ? 1 : 0));
        saveDifficultySettings();
    }

    @Rewrite
    @Override
    public boolean isHardcore() throws IOException {
        if(isCurrent()) return getCurrentWorld().isHardcore();

        return difficultySettingsNbt.getByte("hardcore") == 1;
    }

    @Rewrite
    @Override
    public void setHardcoreEnabled(boolean enabled) throws IOException {
        if(isCurrent()) {
            runner.runTask(() -> getCurrentWorld().setHardcore(enabled));
        }

        difficultySettingsNbt.setByte("hardcore", (byte) (enabled ? 1 : 0));
        saveDifficultySettings();
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
                        FoliaWorldRegion region = new FoliaWorldRegion(server, path);
                        regions.add(region);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }
}
