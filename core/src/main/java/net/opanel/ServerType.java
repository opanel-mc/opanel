package net.opanel;

public enum ServerType {
    BUKKIT("Bukkit"),
    SPIGOT("Spigot"),
    PAPER("Paper"),
    FABRIC("Fabric"),
    FORGE("Forge"),
    NEOFORGE("Neoforge"),
    FOLIA("Folia");

    private final String name;

    ServerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
