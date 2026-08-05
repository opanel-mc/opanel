package net.opanel.update;

import net.opanel.common.OPanelPlugin;
import net.opanel.common.ServerType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface UpdateSourceProvider {
    String getSource();

    boolean supportsAutomaticIdentification();

    boolean isAutoApplySafe();

    List<PluginUpdate> check(
        Path pluginsPath,
        List<OPanelPlugin> plugins,
        Map<String, String> fileHashes,
        String serverVersion,
        ServerType serverType,
        PluginUpdateConfig config
    ) throws IOException;
}
