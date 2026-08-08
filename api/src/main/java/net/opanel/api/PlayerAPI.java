package net.opanel.api;

import java.net.InetAddress;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface PlayerAPI {
    UUID getUuid();
    String getName();

    boolean isOnline();
    boolean isOperator();
    boolean isBanned();

    GameMode getGameMode();
    Position getPosition();

    OptionalInt getPing();
    Optional<InetAddress> getAddress();
    Optional<String> getBanReason();

    /**
     * Updates the player's game mode. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void setGameMode(GameMode gameMode);

    /**
     * Updates the player's operator status. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void setOperator(boolean operator);

    /**
     * Kicks the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void kick();

    /**
     * Kicks the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     */
    void kick(String reason);

    /**
     * Bans the player with an empty reason. This operation may block and must not be called
     * from an extension lifecycle callback or the Minecraft main thread.
     */
    void ban();

    /**
     * Bans the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     */
    void ban(String reason);

    /**
     * Pardons the player. This operation may block and must not be called from an extension
     * lifecycle callback or the Minecraft main thread.
     */
    void pardon();
}
