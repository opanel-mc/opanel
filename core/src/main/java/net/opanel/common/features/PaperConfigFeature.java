package net.opanel.common.features;

import net.opanel.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface PaperConfigFeature {
    Path bukkitConfigPath = Paths.get("").resolve("bukkit.yml");
    Path spigotConfigPath = Paths.get("").resolve("spigot.yml");
    Path legacyPaperConfigPath = Paths.get("").resolve("paper.yml");
    Path paperGlobalConfigPath = Paths.get("").resolve("config/paper-global.yml");
    Path paperWorldDefaultsConfigPath = Paths.get("").resolve("config/paper-world-defaults.yml");
    Path leavesConfigPath = Paths.get("").resolve("leaves.yml");

    private Path getPaperServerConfigPath(String target) throws NoSuchFileException {
        Path targetPath;
        switch(target) {
            case "bukkit" -> targetPath = bukkitConfigPath;
            case "spigot" -> targetPath = spigotConfigPath;
            case "paper" -> targetPath = Files.exists(paperGlobalConfigPath)
                    ? paperGlobalConfigPath
                    : legacyPaperConfigPath;
            case "leaves" -> targetPath = leavesConfigPath;
            default -> throw new IllegalArgumentException("Unknown target name");
        }
        if(!Files.exists(targetPath)) {
            throw new NoSuchFileException("Cannot find the target Paper server config");
        }
        return targetPath;
    }

    private Path getPaperWorldConfigPath(String worldName) throws NoSuchFileException {
        Path targetPath = Paths.get("").resolve(worldName).resolve("paper-world.yml");
        if(!Files.exists(targetPath)) {
            throw new NoSuchFileException("Cannot find the config of world "+ worldName);
        }
        return targetPath;
    }

    default String getPaperServerConfigContent(String target) throws IOException {
        return Utils.readTextFile(getPaperServerConfigPath(target));
    }

    default void writePaperServerConfigContent(String target, String content) throws IOException {
        Utils.writeTextFile(getPaperServerConfigPath(target), content);
    }

    default String getPaperWorldDefaultsConfigContent() throws IOException {
        Path targetPath = Files.exists(paperWorldDefaultsConfigPath)
                ? paperWorldDefaultsConfigPath
                : legacyPaperConfigPath;
        return Utils.readTextFile(targetPath);
    }

    default void writePaperWorldDefaultsConfigContent(String content) throws IOException {
        Path targetPath = Files.exists(paperWorldDefaultsConfigPath)
                ? paperWorldDefaultsConfigPath
                : legacyPaperConfigPath;
        Utils.writeTextFile(targetPath, content);
    }

    default String getPaperWorldConfigContent(String worldName) throws IOException {
        return Utils.readTextFile(getPaperWorldConfigPath(worldName));
    }

    default void writePaperWorldConfigContent(String worldName, String content) throws IOException {
        Utils.writeTextFile(getPaperWorldConfigPath(worldName), content);
    }
}
