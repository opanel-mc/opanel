package net.opanel.api.event;

import net.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when a player leaves the server.
 *
 * <p>The player is a live API handle rather than an immutable snapshot.
 * Availability of online-only data follows the normal {@link PlayerAPI}
 * rules for an offline player.</p>
 */
public final class PlayerLeaveEvent extends ExtensionEvent {
    private final PlayerAPI player;

    public PlayerLeaveEvent(PlayerAPI player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player that left
     */
    public PlayerAPI getPlayer() {
        return player;
    }
}
