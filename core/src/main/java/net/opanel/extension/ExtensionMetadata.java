package net.opanel.extension;

import java.util.List;

public class ExtensionMetadata {
    public String extId;
    public String version;
    public String name;
    public String description;
    public String author;
    public List<ExtensionPage> pages;

    public static class ExtensionPage {
        public String name;
        public String url;
    }
}
