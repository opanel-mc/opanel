package cn.opanel.api.event;

import cn.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired after a player joins the server.
 *
 * <p>The player is a live API handle rather than an immutable snapshot. The
 * handle remains usable while the owning extension is active and subject to
 * the normal {@link PlayerAPI} availability rules.</p>
 */
public final class PlayerJoinEvent extends ExtensionEvent {
    private final PlayerAPI player;

    public PlayerJoinEvent(PlayerAPI player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return the player that joined
     */
    public PlayerAPI getPlayer() {
        return player;
    }
}
