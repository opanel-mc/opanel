package net.opanel.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.opanel.OPanel;
import net.opanel.backup.BackupConfiguration;
import net.opanel.task.ScheduledTask;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Storage {
    private static Storage instance;
    private final Gson gson = new Gson();

    private final HashMap<StorageKey, StorageFile<?>> registeredStorageFiles = new HashMap<>();

    private Storage() {
        registeredStorageFiles.put(StorageKey.SCHEDULED_TASKS, new StorageFile<>(
                "tasks.json",
                new TypeToken<List<ScheduledTask>>() {
                }.getType(),
                new ArrayList<ScheduledTask>()));
        registeredStorageFiles.put(StorageKey.BACKUP_CONFIG, new StorageFile<>(
                resolveBackupConfigPath(),
                BackupConfiguration.class,
                BackupConfiguration.defaultConfig));
    }

    private Path resolveBackupConfigPath() {
        String dir = System.getProperty("opanel.backup.config.dir");
        if (dir != null && !dir.isBlank()) {
            return Paths.get(dir).resolve("backup_config.json");
        }
        return OPanel.OPANEL_DIR_PATH.resolve("backup_config.json");
    }

    @SuppressWarnings("unchecked")
    public <T> T getStoredData(StorageKey key) {
        StorageFile<T> file = (StorageFile<T>) registeredStorageFiles.get(key);
        if (file == null)
            return null;

        try {
            return file.read();
        } catch (IOException e) {
            System.err.println("Cannot read the storage file: " + key.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void setStoredData(StorageKey key, T data) {
        StorageFile<T> file = (StorageFile<T>) registeredStorageFiles.get(key);
        if (file == null)
            return;

        try {
            file.write(data);
        } catch (IOException e) {
            System.err.println("Cannot read the storage file: " + key.toString());
        }
    }

    public static Storage get() {
        if (instance == null)
            instance = new Storage();
        return instance;
    }
}
