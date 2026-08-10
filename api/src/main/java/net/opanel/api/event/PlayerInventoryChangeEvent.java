package net.opanel.api.event;

import net.opanel.api.player.InventoryAPI;
import net.opanel.api.player.PlayerAPI;

import java.util.Objects;

/**
 * Fired when OPanel's inventory poller detects a player inventory change.
 *
 * <p>The player and inventory are live API handles rather than immutable
 * snapshots. The internal inventory hash used to detect changes is not exposed.
 * This event normally runs on OPanel's inventory poller thread.</p>
 */
public final class PlayerInventoryChangeEvent extends ExtensionEvent {
    private final PlayerAPI player;
    private final InventoryAPI inventory;

    public PlayerInventoryChangeEvent(PlayerAPI player, InventoryAPI inventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /**
     * @return the player whose inventory changed
     */
    public PlayerAPI getPlayer() {
        return player;
    }

    /**
     * @return the live inventory handle for the player
     */
    public InventoryAPI getInventory() {
        return inventory;
    }
}
