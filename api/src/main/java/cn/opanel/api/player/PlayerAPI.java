package cn.opanel.api.player;

import cn.opanel.api.exception.InvalidPlayerStateException;
import cn.opanel.api.exception.OperationFailedException;
import cn.opanel.api.exception.PlayerUnavailableException;

import java.net.InetAddress;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Stable handle for a player identified by UUID.
 *
 * <p>Player state is resolved again for every call, so getters represent the
 * latest state rather than a snapshot captured when this handle was created.
 * The handle may represent an offline player when the backing platform can load
 * saved player data. Calls fail with
 * {@link PlayerUnavailableException} when that player
 * is no longer available. Platform failures are wrapped in
 * {@link OperationFailedException}.</p>
 *
 * <p>以 UUID 标识的玩家的稳定句柄。</p>
 *
 * <p>每次调用都会重新解析玩家状态，因此 getter 表示的是最新状态，
 * 而非创建该句柄时捕获的快照。当底层平台能够加载已保存的玩家数据时，
 * 该句柄可以表示一名离线玩家。当该玩家不再可用时，调用会抛出
 * {@link PlayerUnavailableException}。平台失败会被包装在
 * {@link OperationFailedException} 中。</p>
 */
public interface PlayerAPI { // 以 UUID 标识的玩家的稳定句柄
    /**
     * Returns the identity of this player handle.
     *
     * @return the player's UUID
     */
    UUID getUuid(); // 返回该玩家句柄的身份（UUID）

    /**
     * Returns the player's most recently known name.
     *
     * @return the player name
     */
    String getName(); // 返回玩家最近已知的名称

    /**
     * @return {@code true} when the player is currently connected
     */
    boolean isOnline(); // 玩家当前是否在线

    /**
     * @return {@code true} when the player currently has operator privileges
     */
    boolean isOp(); // 玩家当前是否具有操作员（OP）权限

    /**
     * @return {@code true} when the player is banned from the server
     */
    boolean isBanned(); // 玩家是否被服务器封禁

    /**
     * Returns the player's current or saved game mode.
     *
     * @return the game mode
     */
    GameMode getGameMode(); // 返回玩家当前或已保存的游戏模式

    /**
     * Returns the player's current or last saved position.
     *
     * @return an immutable position value
     */
    Position getPosition(); // 返回玩家当前或最后保存的位置

    /**
     * Returns a stable inventory handle bound to this player's UUID.
     *
     * @return the player's inventory API
     */
    InventoryAPI getInventory(); // 返回绑定到该玩家 UUID 的稳定物品栏句柄

    /**
     * Returns the player's current latency when online.
     *
     * @return ping in milliseconds, or an empty value for an offline player
     */
    OptionalInt getPing(); // 返回玩家在线时的当前延迟（毫秒），离线时为空值

    /**
     * Returns the player's network address when online and available from the
     * server implementation.
     *
     * @return the address, or an empty value for an offline/unavailable address
     */
    Optional<InetAddress> getAddress(); // 返回玩家在线且服务器实现可提供时的网络地址

    /**
     * Returns the recorded ban reason when the player is banned and a reason was
     * supplied.
     *
     * @return the ban reason, otherwise an empty value
     */
    Optional<String> getBanReason(); // 返回玩家被封禁且提供了原因时记录的原因

    /**
     * Updates the player's game mode. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param gameMode target game mode
     * @throws NullPointerException if {@code gameMode} is {@code null}
     */
    void setGameMode(GameMode gameMode); // 更新玩家的游戏模式

    /**
     * Updates the player's operator status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param operator {@code true} to grant operator privileges, {@code false} to revoke them
     */
    void setOp(boolean operator); // 更新玩家的操作员状态

    /**
     * Kicks the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @throws InvalidPlayerStateException if the player is offline
     */
    default void kick() { // 以空原因踢出玩家
        kick("");
    }

    /**
     * Kicks the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     *
     * @param reason disconnect reason; an empty string is allowed
     * @throws NullPointerException if {@code reason} is {@code null}
     * @throws InvalidPlayerStateException if the player is offline
     */
    void kick(String reason); // 以指定原因踢出玩家

    /**
     * Bans the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    default void ban() { // 以空原因封禁玩家
        ban("");
    }

    /**
     * Bans the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     *
     * @param reason ban reason; an empty string is allowed
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    void ban(String reason); // 以指定原因封禁玩家

    /**
     * Pardons the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     */
    void pardon(); // 解除对玩家的封禁
}
