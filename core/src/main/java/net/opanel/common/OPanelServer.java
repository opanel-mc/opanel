package net.opanel.common;

import net.opanel.ServerType;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public interface OPanelServer {
    Path serverPropertiesPath = Paths.get("").resolve("server.properties");

    ServerType getServerType();
    byte[] getFavicon();
    String getMotd();
    void setMotd(String motd) throws IOException;
    String getVersion();
    int getPort();
    List<OPanelSave> getSaves();
    OPanelSave getSave(String saveName);
    void saveAll();
    List<OPanelPlayer> getOnlinePlayers();
    List<OPanelPlayer> getPlayers();
    int getMaxPlayerCount();
    OPanelPlayer getPlayer(String uuid);
    boolean isWhitelistEnabled();
    void setWhitelistEnabled(boolean enabled);
    OPanelWhitelist getWhitelist();
    void sendServerCommand(String command);
    List<String> getCommands();
    
    /**
     * Get commands filtered by category
     */
    default List<String> getCommandsByCategory(CommandCategory.Category category) {
        List<String> allCommands = getCommands();
        return CommandCategory.filterByCategory(allCommands, category);
    }
    
    /**
     * Get commands grouped by category
     */
    default Map<CommandCategory.Category, List<String>> getCommandsGrouped() {
        List<String> allCommands = getCommands();
        return CommandCategory.groupByCategory(allCommands);
    }
    
    /**
     * Get commands sorted by priority
     */
    default List<String> getCommandsSorted() {
        List<String> allCommands = getCommands();
        return CommandCategory.sortByPriority(allCommands);
    }
    
    HashMap<String, Object> getGamerules();
    void setGamerules(HashMap<String, Object> gamerules);
    void reload();
    void stop();
    long getIngameTime();

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
