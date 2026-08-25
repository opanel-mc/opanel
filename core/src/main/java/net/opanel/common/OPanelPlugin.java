package net.opanel.common;

import java.util.List;

public record OPanelPlugin(
        String fileName,
        String name,
        String version,
        String description,
        List<String> authors,
        String website,
        byte[] icon,
        long fileSize,
        boolean enabled,
        boolean loaded
) {
    public static final String DISABLED_SUFFIX = ".disabled";

    public OPanelPlugin {
        if(authors != null) authors = List.copyOf(authors);
        if(icon != null) icon = icon.clone();
    }

    @Override
    public byte[] icon() {
        return icon == null ? null : icon.clone();
    }
}
