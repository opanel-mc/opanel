package net.opanel.plugin;

public enum PluginProvider {
    MODRINTH(new ModrinthInstaller()),
    CURSEFORGE(new CurseforgeInstaller());

    private final Installer installerInstance;

    PluginProvider(Installer installer) {
        installerInstance = installer;
    }

    public Installer getInstaller() {
        return installerInstance;
    }

    public static PluginProvider fromId(String id) {
        switch(id) {
            case "modrinth": return MODRINTH;
            case "curseforge": return CURSEFORGE;
        }
        return null;
    }
}
