package net.opanel.event;

import cn.opanel.api.event.ExtensionEvent;
import net.opanel.extension.api.ExtensionAPI;

public abstract class OPanelEvent {
    public abstract ExtensionEvent toAPIEvent(ExtensionAPI api) throws UnsupportedOperationException;
}
