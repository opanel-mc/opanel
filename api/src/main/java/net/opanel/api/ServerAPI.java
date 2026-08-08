package net.opanel.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServerAPI {
    ServerType getServerType();
    String getMinecraftVersion();

    String getMotd();

    /**
     * Updates the server MOTD. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void setMotd(String motd);

    int getPort();
    int getMaxPlayerCount();

    List<PlayerAPI> getOnlinePlayers();
    List<PlayerAPI> getPlayers();
    Optional<PlayerAPI> getPlayer(UUID uniqueId);

    boolean isWhitelistEnabled();

    /**
     * Updates whitelist status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void setWhitelistEnabled(boolean enabled);

    /**
     * Saves all server data. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void saveAll();

    /**
     * Sends a server command. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void sendServerCommand(String command);
}
