package net.opanel.storage;

import com.google.gson.reflect.TypeToken;
import net.opanel.config.MapConfiguration;
import net.opanel.config.McpConfiguration;
import net.opanel.config.OidcConfiguration;
import net.opanel.config.OpenAPIConfiguration;
import net.opanel.monitor.ActivityData;
import net.opanel.task.ScheduledTask;
import net.opanel.task.ScheduledTaskManager;

import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Storage {
    private static Storage instance;

    private final HashMap<StorageKey, StorageFile<?>> registeredStorageFiles = new HashMap<>();

    private Storage() {
        registeredStorageFiles.put(StorageKey.SCHEDULED_TASKS, new StorageFile<>(
            "tasks.json",
            new TypeToken<List<ScheduledTask>>() {}.getType(),
            ScheduledTaskManager.DEFAULT_TASKS
        ));
        registeredStorageFiles.put(StorageKey.MCP_CONFIG, new StorageFile<>(
            "mcp-config.json",
            McpConfiguration.class,
            new McpConfiguration(false)
        ));
        registeredStorageFiles.put(StorageKey.OPEN_API_CONFIG, new StorageFile<>(
            "open-api.json",
            OpenAPIConfiguration.class,
            new OpenAPIConfiguration(false, OpenAPIConfiguration.createDefaultInterfaces())
        ));
        registeredStorageFiles.put(StorageKey.LAUNCH_COMMAND, new StorageFile<>(
            "launch-command.txt",
            ""
        ));
        registeredStorageFiles.put(StorageKey.MAP_CONFIG, new StorageFile<>(
            "map-config.json",
            MapConfiguration.class,
            new MapConfiguration(false)
        ));
        registeredStorageFiles.put(StorageKey.OIDC_CONFIG, new StorageFile<>(
            "oidc-config.json",
            OidcConfiguration.class,
            new OidcConfiguration()
        ));
        registeredStorageFiles.put(StorageKey.ACTIVITY, new StorageFile<>(
            "activity.json",
            new TypeToken<List<ActivityData>>() {}.getType(),
            new ArrayList<ActivityData>()
        ));
    }

    @SuppressWarnings("unchecked")
    public <T> T getStoredData(StorageKey key) {
        StorageFile<T> file = (StorageFile<T>) registeredStorageFiles.get(key);
        if(file == null) return null;

        try {
            return file.read();
        } catch (IOException e) {
            System.err.println("Cannot read the storage file: "+ key.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void setStoredData(StorageKey key, T data) {
        StorageFile<T> file = (StorageFile<T>) registeredStorageFiles.get(key);
        if(file == null) return;

        try {
            file.write(data);
        } catch (IOException e) {
            System.err.println("Cannot read the storage file: "+ key.toString());
        }
    }

    public static Storage get() {
        if(instance == null) instance = new Storage();
        return instance;
    }
}
