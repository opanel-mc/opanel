package net.opanel.common;

import java.util.HashMap;
import java.util.Map;

public record OPanelPlayerPosition(double x, double y, double z, String save) {
    public OPanelPlayerPosition(double x, double y, double z) {
        this(x, y, z, null);
    }

    public Map<String, Object> serialize() {
        HashMap<String, Object> position = new HashMap<>();
        position.put("x", x);
        position.put("y", y);
        position.put("z", z);
        if(save != null) position.put("save", save);
        return position;
    }
}
