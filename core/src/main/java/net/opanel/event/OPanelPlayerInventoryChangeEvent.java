package net.opanel.event;

import net.opanel.common.OPanelPlayer;

import java.util.Map;

public class OPanelPlayerInventoryChangeEvent extends OPanelEvent {
    private final OPanelPlayer player;
    private final Map<String, Object> inventoryData;

    public OPanelPlayerInventoryChangeEvent(OPanelPlayer player, Map<String, Object> inventoryData) {
        this.player = player;
        this.inventoryData = inventoryData;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    public Map<String, Object> getInventoryData() {
        return inventoryData;
    }
}
