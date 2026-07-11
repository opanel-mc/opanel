package net.opanel.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.opanel.OPanel;
import net.opanel.utils.DateAdapter;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Map;

public class StorageFile<T> {
    private final Gson gson;
    private final Path filePath;
    private final Type dataType;
    private final T defaultValue;
    private final JsonElement defaultJsonTree;
    private final boolean isPlainText;

    public StorageFile(String fileName, Type dataType, T defaultValue) {
        this(
            fileName,
            dataType,
            createGsonBuilder().create(),
            defaultValue
        );
    }

    public StorageFile(String fileName, Type dataType, Object typeAdapter, T defaultValue) {
        this(
            fileName,
            dataType,
            createGsonBuilder()
                .registerTypeAdapter(dataType, typeAdapter)
                .create(),
            defaultValue
        );
    }

    private StorageFile(String fileName, Type dataType, Gson gson, T defaultValue) {
        filePath = OPanel.OPANEL_DIR_PATH.resolve(fileName);
        this.dataType = dataType;
        this.gson = gson;
        this.defaultValue = defaultValue;
        this.defaultJsonTree = gson.toJsonTree(defaultValue, dataType);
        isPlainText = false;

        if(!Files.exists(filePath)) {
            try {
                Files.writeString(filePath, gson.toJson(defaultValue), StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public StorageFile(String fileName, String defaultValue) {
        filePath = OPanel.OPANEL_DIR_PATH.resolve(fileName);
        dataType = null;
        gson = null;
        this.defaultValue = (T) defaultValue;
        defaultJsonTree = null;
        isPlainText = true;

        if(!Files.exists(filePath)) {
            try {
                Files.writeString(filePath, defaultValue, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static GsonBuilder createGsonBuilder() {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateAdapter())
                .setPrettyPrinting();
    }

    @SuppressWarnings("unchecked")
    public T read() throws IOException {
        String rawText = Utils.readTextFile(filePath);
        if(isPlainText) return (T) rawText;

        JsonElement jsonTree;

        try {
            jsonTree = JsonParser.parseString(rawText);
        } catch (JsonSyntaxException e) {
            write(defaultValue);
            return defaultValue;
        }

        if(jsonTree == null || jsonTree.isJsonNull()) {
            write(defaultValue);
            return defaultValue;
        }

        if(fillMissingValues(jsonTree, defaultJsonTree)) {
            Files.writeString(filePath, gson.toJson(jsonTree), StandardOpenOption.TRUNCATE_EXISTING);
        }

        return gson.fromJson(jsonTree, dataType);
    }

    public void write(T obj) throws IOException {
        String jsonText = isPlainText ? (String) obj : gson.toJson(obj);
        Files.writeString(filePath, jsonText, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private boolean fillMissingValues(JsonElement target, JsonElement defaults) {
        if(!target.isJsonObject() || !defaults.isJsonObject()) return false;

        JsonObject targetObject = target.getAsJsonObject();
        JsonObject defaultObject = defaults.getAsJsonObject();
        boolean updated = false;

        for(Map.Entry<String, JsonElement> entry : defaultObject.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();

            if(!targetObject.has(key)) {
                targetObject.add(key, defaultValue.deepCopy());
                updated = true;
                continue;
            }

            JsonElement targetValue = targetObject.get(key);
            if(fillMissingValues(targetValue, defaultValue)) {
                updated = true;
            }
        }

        return updated;
    }
}
