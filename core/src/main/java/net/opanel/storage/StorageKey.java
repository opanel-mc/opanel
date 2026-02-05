package net.opanel.storage;

public enum StorageKey {
    SCHEDULED_TASKS("scheduled-tasks"),
    BACKUP_CONFIG("backup-config");

    private final String id;

    StorageKey(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
