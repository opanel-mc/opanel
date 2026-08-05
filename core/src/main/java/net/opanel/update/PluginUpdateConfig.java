package net.opanel.update;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginUpdateConfig {
    private Map<String, PluginUpdateBinding> bindings = new LinkedHashMap<>();

    public PluginUpdateConfig() {
    }

    public Map<String, PluginUpdateBinding> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, PluginUpdateBinding> bindings) {
        this.bindings = bindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bindings);
    }

    public PluginUpdateBinding getBinding(String fileName) {
        return bindings.get(fileName);
    }

    public void setBinding(PluginUpdateBinding binding) {
        if(binding != null && binding.getFileName() != null) {
            bindings.put(binding.getFileName(), binding);
        }
    }

    public void removeBinding(String fileName) {
        bindings.remove(fileName);
    }

    public boolean hasBinding(String fileName) {
        return bindings.containsKey(fileName);
    }

    public PluginUpdateConfig copy() {
        PluginUpdateConfig copy = new PluginUpdateConfig();
        copy.setBindings(new LinkedHashMap<>(bindings));
        return copy;
    }
}
