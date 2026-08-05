package net.opanel.update;

import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;

import java.util.LinkedHashMap;

public class PluginUpdateConfigStore {
    public PluginUpdateConfig getConfig() {
        PluginUpdateConfig config = Storage.get().getStoredData(StorageKey.PLUGIN_UPDATE_CONFIG);
        return config == null ? new PluginUpdateConfig() : config;
    }

    public PluginUpdateBinding getBinding(String fileName) {
        return getConfig().getBinding(fileName);
    }

    public void setBinding(PluginUpdateBinding binding) {
        PluginUpdateConfig config = getConfig();
        config.setBinding(binding);
        save(config);
    }

    public void setBindingIfAbsent(String fileName, PluginUpdateBinding binding) {
        PluginUpdateConfig config = getConfig();
        if(!config.hasBinding(fileName)) {
            PluginUpdateBinding copy = new PluginUpdateBinding(
                fileName,
                binding.getSource(),
                binding.getProjectId(),
                binding.getOwner(),
                binding.getRepo(),
                binding.getAssetPattern(),
                binding.getChannels() == null ? null : new java.util.ArrayList<>(binding.getChannels())
            );
            config.setBinding(copy);
            save(config);
        }
    }

    public void removeBinding(String fileName) {
        PluginUpdateConfig config = getConfig();
        config.removeBinding(fileName);
        save(config);
    }

    private void save(PluginUpdateConfig config) {
        Storage.get().setStoredData(StorageKey.PLUGIN_UPDATE_CONFIG, config);
    }

    public java.util.Map<String, PluginUpdateBinding> getBindingsSnapshot() {
        return new LinkedHashMap<>(getConfig().getBindings());
    }
}
