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
 *
 * <p>对正在运行的 Minecraft 服务器及服务器级功能的访问。</p>
 *
 * <p>这是一个稳定句柄，而返回的集合与 Map 都是不可变快照。需要活动服务器
 * 的操作，在 OPanel 未附加到服务器或服务器正在关闭时，会抛出
 * {@link ServerUnavailableException}。其他平台失败会以
 * {@link OperationFailedException} 的形式暴露。</p>
 */
public interface ServerAPI { // 对正在运行的 Minecraft 服务器及服务器级功能的访问
    /**
     * @return the server platform family
     */
    ServerType getServerType(); // 返回服务器平台家族

    /**
     * @return the Minecraft version reported by the server implementation
     */
    String getMinecraftVersion(); // 返回服务器实现报告的 Minecraft 版本

    /**
     * @return the current server message of the day, including any Minecraft
     *         formatting codes
     */
    String getMotd(); // 返回当前服务器 MOTD（含 Minecraft 格式化代码）

    /**
     * Updates the server MOTD. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param motd new message of the day
     * @throws NullPointerException if {@code motd} is {@code null}
     */
    void setMotd(String motd); // 更新服务器 MOTD

    /**
     * @return the configured Minecraft server port
     */
    int getPort(); // 返回配置的 Minecraft 服务器端口

    /**
     * @return the configured maximum number of concurrent players
     */
    int getMaxPlayerCount(); // 返回配置的最大并发玩家数

    /**
     * Returns handles for the players currently connected to the server.
     *
     * @return an unmodifiable snapshot list of online player handles
     */
    List<PlayerAPI> getOnlinePlayers(); // 返回当前在线玩家的句柄列表

    /**
     * Returns handles for every player profile known to the server, including
     * offline players when supported by the platform.
     *
     * @return an unmodifiable snapshot list of player handles
     */
    List<PlayerAPI> getPlayers(); // 返回服务器已知的所有玩家档案句柄（含离线玩家）

    /**
     * Resolves a player by UUID.
     *
     * @param uniqueId player UUID
     * @return a player handle, or an empty value when the player is unknown
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    Optional<PlayerAPI> getPlayer(UUID uniqueId); // 按 UUID 解析玩家

    /**
     * Returns the game rules for the Overworld.
     *
     * <p>This is equivalent to calling
     * {@link #getGamerules(Dimension) getGamerules(Dimension.OVERWORLD)}.</p>
     *
     * @return an unmodifiable snapshot of Overworld rule names and values
     */
    default Map<String, Object> getGamerules() { // 返回主世界的游戏规则
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
    Map<String, Object> getGamerules(Dimension dimension); // 返回某个维度的游戏规则

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
    default void setGamerules(Map<String, Object> gamerules) { // 更新主世界的游戏规则
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
    void setGamerules(Dimension dimension, Map<String, Object> gamerules); // 更新某个维度的游戏规则

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
    default void setGamerule(String key, Object value) { // 更新主世界的单条游戏规则
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
    void setGamerule(Dimension dimension, String key, Object value); // 更新某个维度的单条游戏规则

    /**
     * @return {@code true} when the server whitelist is enabled
     */
    boolean isWhitelistEnabled(); // 服务器白名单是否启用

    /**
     * Updates whitelist status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param enabled {@code true} to enforce the whitelist, {@code false} to disable it
     */
    void setWhitelistEnabled(boolean enabled); // 更新白名单状态

    /**
     * Saves all server data. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void saveAll(); // 保存所有服务器数据

    /**
     * Sends a server command. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param command non-blank command text to dispatch as the server
     * @throws NullPointerException if {@code command} is {@code null}
     * @throws IllegalArgumentException if {@code command} is blank
     */
    void sendServerCommand(String command); // 以服务器身份发送命令
}
