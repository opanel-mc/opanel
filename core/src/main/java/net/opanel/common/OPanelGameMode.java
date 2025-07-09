package net.opanel.common;

public enum OPanelGameMode {
    ADVENTURE("adventure"),
    SURVIVAL("survival"),
    CREATIVE("creative"),
    SPECTATOR("spectator");

    private final String name;

    OPanelGameMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
