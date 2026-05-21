package net.opanel.storage;

public enum StorageKey {
    SCHEDULED_TASKS("scheduled-tasks"),
    MCP_CONFIG("mcp-config"),
    OPEN_API_CONFIG("open-api"),
    LAUNCH_COMMAND("launch-command"),
    MAP_CONFIG("map-config");

    private final String id;

    StorageKey(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
