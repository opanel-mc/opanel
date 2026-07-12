package net.opanel.common;

import net.opanel.OPanel;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public interface OPanelServer {
    Path serverPropertiesPath = Paths.get("").resolve("server.properties");
    Path serverIconPath = Paths.get("").resolve("server-icon.png");

    ServerType getServerType();

    default byte[] getFavicon() {
        if(!Files.exists(serverIconPath)) return null;
        try {
            return Files.readAllBytes(serverIconPath);
        } catch (IOException e) {
            return null;
        }
    }

    default void setFavicon(byte[] iconBytes) throws IOException {
        Files.write(serverIconPath, iconBytes);
    }

    String getMotd();
    void setMotd(String motd) throws IOException;
    String getVersion();
    int getPort();
    List<OPanelSave> getSaves();
    OPanelSave getSave(String saveName);
    String getCurrentSaveName();
    void saveAll();
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    int getMaxPlayerCount();
    OPanelPlayer getPlayer(String uuid);
    void removePlayerData(String uuid) throws IOException;
    List<String> getBannedIps();
    void banIp(String ip) throws UnknownHostException;
    void pardonIp(String ip) throws UnknownHostException;
    boolean isWhitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    OPanelWhitelist getWhitelist();
    void sendServerCommand(String command);
    List<String> getCommands();
    List<String> getCommandTabList(int argIndex, String command);
    HashMap<String, Object> getGamerules(OPanelDimension dimension);
    void setGamerules(OPanelDimension dimension, HashMap<String, Object> gamerules);
    void reload();
    void stop();

    default void restart(int delay) {
        final String launchCommand = Storage.get().getStoredData(StorageKey.LAUNCH_COMMAND);

        try {
            final Path cwd = Path.of(".").toRealPath();

            String os = System.getProperty("os.name").toLowerCase();
            String[] command;
            if(os.contains("win")) { // windows
                command = new String[] { "cmd.exe", "/c", "start", "", "cmd.exe", "/c", "timeout "+ delay +" > NUL && "+ launchCommand };
            } else if(os.contains("mac")) { // mac
                // create launch script file
                final String scriptContent = new StringBuilder()
                    .append("#!/bin/bash").append("\n")
                    .append("sleep ").append(delay).append("\n")
                    .append("cd \"").append(cwd.toAbsolutePath()).append("\"").append("\n")
                    .append(launchCommand).append("\n")
                    .append("rm -- \"$0\"")
                    .toString();
                Path scriptPath = OPanel.TMP_DIR_PATH.resolve("temp_restart.command").toAbsolutePath();
                Utils.writeTextFile(scriptPath, scriptContent);

                // grant access permission
                Process chmodProcess = new ProcessBuilder("chmod", "+x", scriptPath.toString()).start();
                chmodProcess.waitFor();

                command = new String[] { "open", scriptPath.toString() };
            } else { // linux / other servers
                final String safeLaunchCommand = launchCommand.replace("'", "'\\''");
                command = new String[] { "bash", "-c", "nohup bash -c 'sleep "+ delay +" && "+ safeLaunchCommand +"'" };
            }

            new ProcessBuilder(command)
                .directory(cwd.toFile())
                .start();
            stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    long getIngameTime();
    Path getPluginsPath();
    List<OPanelPlugin> getPlugins();
    void togglePlugin(String fileName, boolean enabled) throws IOException;
    void deletePlugin(String fileName) throws IOException;
    OPanelChunkAccessor getChunkAccessor();

    static String getPropertiesContent() throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        return Utils.readTextFile(serverPropertiesPath);
    }

    static void writePropertiesContent(String newContent) throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        Utils.writeTextFile(serverPropertiesPath, newContent);
    }
}
