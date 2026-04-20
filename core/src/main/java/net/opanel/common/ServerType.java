package net.opanel.common;

public enum ServerType {
    BUKKIT("Bukkit"),
    SPIGOT("Spigot"),
    PAPER("Paper"),
    FABRIC("Fabric"),
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    FOLIA("Folia"),
    LEAVES("Leaves");

    private final String name;

    ServerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isBukkitSeries() {
        return (
                this == BUKKIT
                || this == SPIGOT
                || this == PAPER
                || this == FOLIA
                || this == LEAVES
        );
    }
}
