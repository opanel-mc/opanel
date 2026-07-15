package net.opanel.common;

public enum ServerType {
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

    public boolean isPaperSeries() {
        return (
                this == PAPER
                || this == FOLIA
                || this == LEAVES
        );
    }
}
