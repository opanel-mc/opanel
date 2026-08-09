package net.opanel.api.server;

public enum Dimension {
    OVERWORLD("overworld"),
    NETHER("nether"),
    THE_END("the_end");

    private final String name;

    Dimension(String name) {
        this.name = name;
    }

    /**
     * Returns the stable serialized name used by OPanel protocols.
     *
     * @return the serialized dimension name
     */
    public String getName() {
        return name;
    }
}
