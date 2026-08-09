package net.opanel.event;

import net.opanel.api.event.ExtensionEvent;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelChunkDirtyEvent extends OPanelEvent {
    private final int chunkX;
    private final int chunkZ;

    public OPanelChunkDirtyEvent(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public ExtensionEvent toAPIEvent(ExtensionAPI api) {
        throw new UnsupportedOperationException("OPanelChunkDirtyEvent is not open to extension API.");
    }
}
