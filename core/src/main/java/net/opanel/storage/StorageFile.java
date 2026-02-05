package net.opanel.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.opanel.OPanel;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StorageFile<T> {
    private final Gson gson;
    private final Path filePath;
    private final Type dataType;

    public StorageFile(String fileName, Type dataType, T defaultValue) {
        this(
            fileName,
            dataType,
            new GsonBuilder()
                .setPrettyPrinting()
                .create(),
            defaultValue
        );
    }

    public StorageFile(String fileName, Type dataType, Object typeAdapter, T defaultValue) {
        this(
            fileName,
            dataType,
            new GsonBuilder()
                .registerTypeAdapter(dataType, typeAdapter)
                .setPrettyPrinting()
                .create(),
            defaultValue
        );
    }

    public StorageFile(Path filePath, Type dataType, T defaultValue) {
        this(
            filePath,
            dataType,
            new GsonBuilder()
                .setPrettyPrinting()
                .create(),
            defaultValue
        );
    }

    private StorageFile(String fileName, Type dataType, Gson gson, T defaultValue) {
        this(OPanel.OPANEL_DIR_PATH.resolve(fileName), dataType, gson, defaultValue);
    }

    private StorageFile(Path filePath, Type dataType, Gson gson, T defaultValue) {
        this.filePath = filePath;
        this.dataType = dataType;
        this.gson = gson;

        if (!Files.exists(filePath)) {
            try {
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(filePath, gson.toJson(defaultValue), StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public T read() throws IOException {
        String rawText = Utils.readTextFile(filePath);
        return gson.fromJson(rawText, dataType);
    }

    public void write(T obj) throws IOException {
        String jsonText = gson.toJson(obj);
        Files.writeString(filePath, jsonText, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
