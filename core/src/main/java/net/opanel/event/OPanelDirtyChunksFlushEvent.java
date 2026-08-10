package net.opanel.event;

import net.opanel.api.event.ExtensionEvent;
import net.opanel.extension.api.ExtensionAPI;

import java.util.Set;

public class OPanelDirtyChunksFlushEvent extends OPanelEvent {
    private final String saveName;
    private final Set<int[]> flushedChunks;

    public OPanelDirtyChunksFlushEvent(String saveName, Set<int[]> flushedChunks) {
        this.saveName = saveName;
        this.flushedChunks = flushedChunks;
    }

    public String getSaveName() {
        return saveName;
    }

    public Set<int[]> getFlushedChunks() {
        return flushedChunks;
    }

    @Override
    public ExtensionEvent toAPIEvent(ExtensionAPI api) {
        throw new UnsupportedOperationException("OPanelDirtyChunksFlushEvent is not open to extension API.");
    }
}
