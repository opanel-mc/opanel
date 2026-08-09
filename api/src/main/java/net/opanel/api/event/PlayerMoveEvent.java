package net.opanel.api.event;

import net.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when OPanel detects that a player has moved.
 *
 * <p>This event does not contain old and new position snapshots. The player's
 * current position can be read through {@link PlayerAPI#getPosition()} while
 * handling the event. This is a high-frequency event, so handlers must remain
 * lightweight.</p>
 */
public final class PlayerMoveEvent extends ExtensionEvent {
    private final PlayerAPI player;

    public PlayerMoveEvent(PlayerAPI player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player whose position changed
     */
    public PlayerAPI getPlayer() {
        return player;
    }
}
