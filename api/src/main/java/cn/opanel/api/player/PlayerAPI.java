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
 */
public interface PlayerAPI {
    /**
     * Returns the identity of this player handle.
     *
     * @return the player's UUID
     */
    UUID getUuid();

    /**
     * Returns the player's most recently known name.
     *
     * @return the player name
     */
    String getName();

    /**
     * @return {@code true} when the player is currently connected
     */
    boolean isOnline();

    /**
     * @return {@code true} when the player currently has operator privileges
     */
    boolean isOp();

    /**
     * @return {@code true} when the player is banned from the server
     */
    boolean isBanned();

    /**
     * Returns the player's current or saved game mode.
     *
     * @return the game mode
     */
    GameMode getGameMode();

    /**
     * Returns the player's current or last saved position.
     *
     * @return an immutable position value
     */
    Position getPosition();

    /**
     * Returns a stable inventory handle bound to this player's UUID.
     *
     * @return the player's inventory API
     */
    InventoryAPI getInventory();

    /**
     * Returns the player's current latency when online.
     *
     * @return ping in milliseconds, or an empty value for an offline player
     */
    OptionalInt getPing();

    /**
     * Returns the player's network address when online and available from the
     * server implementation.
     *
     * @return the address, or an empty value for an offline/unavailable address
     */
    Optional<InetAddress> getAddress();

    /**
     * Returns the recorded ban reason when the player is banned and a reason was
     * supplied.
     *
     * @return the ban reason, otherwise an empty value
     */
    Optional<String> getBanReason();

    /**
     * Updates the player's game mode. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param gameMode target game mode
     * @throws NullPointerException if {@code gameMode} is {@code null}
     */
    void setGameMode(GameMode gameMode);

    /**
     * Updates the player's operator status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @param operator {@code true} to grant operator privileges, {@code false} to revoke them
     */
    void setOp(boolean operator);

    /**
     * Kicks the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     *
     * @throws InvalidPlayerStateException if the player is offline
     */
    default void kick() {
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
    void kick(String reason);

    /**
     * Bans the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    default void ban() {
        ban("");
    }

    /**
     * Bans the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     *
     * @param reason ban reason; an empty string is allowed
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    void ban(String reason);

    /**
     * Pardons the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     */
    void pardon();
}
