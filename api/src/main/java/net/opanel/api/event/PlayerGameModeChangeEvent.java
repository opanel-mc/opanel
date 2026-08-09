package net.opanel.api.event;

import net.opanel.api.player.GameMode;
import net.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when a player's game mode changes.
 *
 * <p>The event exposes the new game mode. The previous game mode is not
 * retained by the extension event API.</p>
 */
public final class PlayerGameModeChangeEvent extends ExtensionEvent {
    private final PlayerAPI player;
    private final GameMode gameMode;

    public PlayerGameModeChangeEvent(PlayerAPI player, GameMode gameMode) {
        this.player = Objects.requireNonNull(player, "player");
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
    }

    /**
     * @return the player whose game mode changed
     */
    public PlayerAPI getPlayer() {
        return player;
    }

    /**
     * @return the player's new game mode
     */
    public GameMode getGameMode() {
        return gameMode;
    }
}
