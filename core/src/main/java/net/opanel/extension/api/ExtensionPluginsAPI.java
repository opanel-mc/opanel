package net.opanel.extension.api;

import cn.opanel.api.exception.ActLaterException;
import cn.opanel.api.plugins.PluginInfo;
import cn.opanel.api.plugins.PluginsAPI;
import net.opanel.common.OPanelPlugin;
import net.opanel.extension.ExtensionContext;
import net.opanel.utils.Utils;

import java.util.*;

public final class ExtensionPluginsAPI implements PluginsAPI {
    private final ExtensionContext ctx;

    ExtensionPluginsAPI(ExtensionContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return ctx.call("get plugins", () -> {
            List<PluginInfo> plugins = new ArrayList<>();
            for(OPanelPlugin plugin : ctx.getServer().getPlugins()) {
                plugins.add(toPluginInfo(plugin));
            }
            return Collections.unmodifiableList(plugins);
        });
    }

    @Override
    public Optional<PluginInfo> getPlugin(String fileName) {
        validateFileName(fileName);
        return ctx.call("get plugin", () -> {
            for(OPanelPlugin plugin : ctx.getServer().getPlugins()) {
                if(fileName.equals(plugin.fileName())) {
                    return Optional.of(toPluginInfo(plugin));
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public void setEnabled(String fileName, boolean enabled) throws ActLaterException {
        validateFileName(fileName);
        ctx.run("set plugin enabled status", () -> {
            try {
                ctx.getServer().togglePlugin(fileName, enabled);
            } catch (net.opanel.exception.ActLaterException e) {
                throw new ActLaterException();
            }
        });
    }

    @Override
    public void deletePlugin(String fileName) throws ActLaterException {
        validateFileName(fileName);
        ctx.run("delete plugin", () -> {
            try {
                ctx.getServer().deletePlugin(fileName);
            } catch (net.opanel.exception.ActLaterException e) {
                throw new ActLaterException();
            }
        });
    }

    private static void validateFileName(String fileName) {
        Objects.requireNonNull(fileName, "fileName");
        if(!Utils.isSafeFileName(fileName) || (
                !fileName.endsWith(".jar")
                && !fileName.endsWith(".jar" + OPanelPlugin.DISABLED_SUFFIX)
        )) {
            throw new IllegalArgumentException("Invalid plugin file name: " + fileName);
        }
    }

    private static PluginInfo toPluginInfo(OPanelPlugin plugin) {
        return new PluginInfo(
                plugin.fileName(),
                plugin.name(),
                plugin.version(),
                plugin.description(),
                plugin.authors(),
                plugin.website(),
                plugin.icon(),
                plugin.fileSize(),
                plugin.enabled(),
                plugin.loaded()
        );
    }
}
