package net.opanel.extension;

import java.util.List;

public record ExtensionMetadata(
        String extId,
        String version,
        String name,
        String description,
        String author,
        List<ExtensionPage> pages
) {
    public ExtensionMetadata {
        if(pages == null) pages = List.of();
    }

    public record ExtensionPage(String name, String url) {}
}
