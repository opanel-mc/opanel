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

    public static OPanelGameMode fromString(String gamemode) {
        switch(gamemode) {
            case "adventure" -> { return ADVENTURE; }
            case "survival" -> { return SURVIVAL; }
            case "creative" -> { return CREATIVE; }
            case "spectator" -> { return SPECTATOR; }
        }
        return null;
    }
}
