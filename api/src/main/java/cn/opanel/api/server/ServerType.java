package cn.opanel.api.server;

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

    /**
     * Returns the human-readable platform name.
     *
     * @return display name
     */
    public String getName() {
        return name;
    }
}
