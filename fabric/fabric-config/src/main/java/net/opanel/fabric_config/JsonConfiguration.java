package net.opanel.fabric_config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public class JsonConfiguration<T> {
    private final Path path;
    private final Class<T> configType;
    private final Gson gson;

    private T config;

    public JsonConfiguration(Path path, T defaultConfig, Class<T> configType) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.configType = Objects.requireNonNull(configType, "configType");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load(Objects.requireNonNull(defaultConfig, "defaultConfig"));
    }

    public synchronized T get() {
        return config;
    }

    public synchronized void set(T config) {
        if(!configType.isInstance(config)) {
            throw new ConfigException("Configuration must be an instance of "+ configType.getName());
        }
        this.config = config;
    }

    public synchronized void save() {
        final String content;
        try {
            content = gson.toJson(config);
        } catch (RuntimeException e) {
            throw new ConfigException("Failed to serialize configuration "+ path, e);
        }
        write(content);
    }

    public Path getPath() {
        return path;
    }

    private void load(T defaultConfig) {
        final JsonElement defaultJson;
        try {
            defaultJson = gson.toJsonTree(defaultConfig);
        } catch (RuntimeException e) {
            throw new ConfigException("Failed to serialize default configuration for "+ path, e);
        }

        if(!Files.exists(path)) {
            config = deserialize(defaultJson.deepCopy());
            save();
            return;
        }

        final String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigException("Failed to read configuration "+ path, e);
        }

        final JsonElement loadedJson;
        try {
            loadedJson = JsonParser.parseString(content);
        } catch (JsonParseException e) {
            throw new ConfigException("Invalid JSON in configuration "+ path, e);
        }

        boolean changed = mergeDefaults(loadedJson, defaultJson, "$");
        config = deserialize(loadedJson);
        if(changed) save();
    }

    private T deserialize(JsonElement json) {
        try {
            T result = gson.fromJson(json, configType);
            if(result == null) {
                throw new ConfigException("Configuration root cannot be null: "+ path);
            }
            return result;
        } catch (JsonParseException | IllegalStateException e) {
            throw new ConfigException("Configuration does not match "+ configType.getName() +": "+ path, e);
        }
    }

    private boolean mergeDefaults(JsonElement loaded, JsonElement defaults, String jsonPath) {
        validateType(loaded, defaults, jsonPath);
        if(!defaults.isJsonObject()) return false;

        JsonObject loadedObject = loaded.getAsJsonObject();
        boolean changed = false;
        for(Map.Entry<String, JsonElement> entry : defaults.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();
            if(!loadedObject.has(key)) {
                loadedObject.add(key, defaultValue.deepCopy());
                changed = true;
            } else {
                changed |= mergeDefaults(loadedObject.get(key), defaultValue, jsonPath +"."+ key);
            }
        }
        return changed;
    }

    private void validateType(JsonElement loaded, JsonElement defaults, String jsonPath) {
        if(defaults.isJsonNull()) return;
        if(loaded == null || loaded.isJsonNull()) {
            throw typeError(jsonPath, defaults, loaded);
        }

        boolean compatible;
        if(defaults.isJsonObject()) {
            compatible = loaded.isJsonObject();
        } else if(defaults.isJsonArray()) {
            compatible = loaded.isJsonArray();
        } else if(defaults.getAsJsonPrimitive().isBoolean()) {
            compatible = loaded.isJsonPrimitive() && loaded.getAsJsonPrimitive().isBoolean();
        } else if(defaults.getAsJsonPrimitive().isNumber()) {
            compatible = loaded.isJsonPrimitive() && loaded.getAsJsonPrimitive().isNumber();
        } else {
            compatible = loaded.isJsonPrimitive() && loaded.getAsJsonPrimitive().isString();
        }

        if(!compatible) throw typeError(jsonPath, defaults, loaded);
    }

    private ConfigException typeError(String jsonPath, JsonElement expected, JsonElement actual) {
        return new ConfigException(
                "Invalid value type at "+ jsonPath +" in "+ path
                        +"; expected "+ typeName(expected) +", found "+ typeName(actual)
        );
    }

    private String typeName(JsonElement element) {
        if(element == null) return "missing";
        if(element.isJsonNull()) return "null";
        if(element.isJsonObject()) return "object";
        if(element.isJsonArray()) return "array";
        if(element.getAsJsonPrimitive().isBoolean()) return "boolean";
        if(element.getAsJsonPrimitive().isNumber()) return "number";
        return "string";
    }

    private void write(String content) {
        Path parent = path.getParent();
        Path tempFile = null;
        try {
            Files.createDirectories(parent);
            tempFile = Files.createTempFile(parent, path.getFileName().toString() +".", ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(
                        tempFile,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ConfigException("Failed to save configuration "+ path, e);
        } finally {
            if(tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    //
                }
            }
        }
    }
}
