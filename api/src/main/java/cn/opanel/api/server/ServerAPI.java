package cn.opanel.api.server;

import cn.opanel.api.exception.OperationFailedException;
import cn.opanel.api.exception.ServerUnavailableException;
import cn.opanel.api.player.PlayerAPI;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Access to the running Minecraft server and server-scoped features.
 *
 * <p>This is a stable handle, while returned collections and maps are immutable
 * snapshots. Operations that require a live server fail with
 * {@link ServerUnavailableException} when OPanel has
 * not attached to a server or the server is shutting down. Other platform
 * failures are exposed as {@link OperationFailedException}.</p>
 */
public interface ServerAPI {
    /**
     * @return the server platform family
     */
    ServerType getServerType();

    /**
     * @return the Minecraft version reported by the server implementation
     */
    String getMinecraftVersion();

    /**
     * @return the current server message of the day, including any Minecraft
     *         formatting codes
     */
    String getMotd();

    /**
     * Updates the server MOTD. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param motd new message of the day
     * @throws NullPointerException if {@code motd} is {@code null}
     */
    void setMotd(String motd);

    /**
     * @return the configured Minecraft server port
     */
    int getPort();

    /**
     * @return the configured maximum number of concurrent players
     */
    int getMaxPlayerCount();

    /**
     * Returns handles for the players currently connected to the server.
     *
     * @return an unmodifiable snapshot list of online player handles
     */
    List<PlayerAPI> getOnlinePlayers();

    /**
     * Returns handles for every player profile known to the server, including
     * offline players when supported by the platform.
     *
     * @return an unmodifiable snapshot list of player handles
     */
    List<PlayerAPI> getPlayers();

    /**
     * Resolves a player by UUID.
     *
     * @param uniqueId player UUID
     * @return a player handle, or an empty value when the player is unknown
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    Optional<PlayerAPI> getPlayer(UUID uniqueId);

    /**
     * Returns the game rules for the Overworld.
     *
     * <p>This is equivalent to calling
     * {@link #getGamerules(Dimension) getGamerules(Dimension.OVERWORLD)}.</p>
     *
     * @return an unmodifiable snapshot of Overworld rule names and values
     */
    default Map<String, Object> getGamerules() {
        return getGamerules(Dimension.OVERWORLD);
    }

    /**
     * Returns the game rules for one dimension.
     *
     * <p>Rule names are Minecraft game-rule keys. Values are normally
     * {@link Boolean} or numeric values depending on the rule and server
     * version.</p>
     *
     * @param dimension dimension to inspect
     * @return an unmodifiable snapshot of rule names and values
     * @throws NullPointerException if {@code dimension} is {@code null}
     */
    Map<String, Object> getGamerules(Dimension dimension);

    /**
     * Updates the supplied Overworld game rules. Rules omitted from the map keep
     * their current values.
     *
     * <p>This is equivalent to calling
     * {@link #setGamerules(Dimension, Map)
     * setGamerules(Dimension.OVERWORLD, gamerules)}. This operation may block
     * and must not be called from an extension lifecycle callback or the
     * Minecraft main thread.</p>
     *
     * @param gamerules rule names and replacement values; omitted rules are preserved
     * @throws NullPointerException if the map, a key, or a value is {@code null}
     * @throws IllegalArgumentException if a rule key is blank
     */
    default void setGamerules(Map<String, Object> gamerules) {
        setGamerules(Dimension.OVERWORLD, gamerules);
    }

    /**
     * Updates the supplied game rules in a dimension. Rules omitted from the map
     * keep their current values. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param dimension dimension whose rules should be updated
     * @param gamerules rule names and replacement values; omitted rules are preserved
     * @throws NullPointerException if an argument, key, or value is {@code null}
     * @throws IllegalArgumentException if a rule key is blank
     */
    void setGamerules(Dimension dimension, Map<String, Object> gamerules);

    /**
     * Updates one Overworld game rule.
     *
     * <p>This is equivalent to calling
     * {@link #setGamerule(Dimension, String, Object)
     * setGamerule(Dimension.OVERWORLD, key, value)}. This operation may block
     * and must not be called from an extension lifecycle callback or the
     * Minecraft main thread.</p>
     *
     * @param key Minecraft game-rule key
     * @param value replacement value compatible with the rule's type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws OperationFailedException if the rule is
     *         unknown or the platform rejects the supplied value
     */
    default void setGamerule(String key, Object value) {
        setGamerule(Dimension.OVERWORLD, key, value);
    }

    /**
     * Updates one game rule in a dimension. This operation may block and must not
     * be called from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param dimension dimension whose rule should be updated
     * @param key Minecraft game-rule key
     * @param value replacement value compatible with the rule's type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws OperationFailedException if the rule is
     *         unknown or the platform rejects the supplied value
     */
    void setGamerule(Dimension dimension, String key, Object value);

    /**
     * @return {@code true} when the server whitelist is enabled
     */
    boolean isWhitelistEnabled();

    /**
     * Updates whitelist status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param enabled {@code true} to enforce the whitelist, {@code false} to disable it
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
     *
     * @param command non-blank command text to dispatch as the server
     * @throws NullPointerException if {@code command} is {@code null}
     * @throws IllegalArgumentException if {@code command} is blank
     */
    void sendServerCommand(String command);
}
