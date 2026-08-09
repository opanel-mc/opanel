package net.opanel.extension;

import net.opanel.OPanel;
import net.opanel.api.exception.APIUnavailableException;
import net.opanel.api.exception.ActLaterException;
import net.opanel.api.exception.OPanelAPIException;
import net.opanel.api.exception.OperationFailedException;
import net.opanel.api.exception.PlayerUnavailableException;
import net.opanel.api.exception.ServerUnavailableException;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.extension.api.ExtensionAPI;

import java.util.Objects;
import java.util.UUID;

public final class ExtensionContext {
    private final OPanel plugin;
    private final ExtensionMetadata metadata;
    private boolean active = true;

    private ExtensionContext(OPanel plugin, ExtensionMetadata metadata) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public synchronized void invalidate() {
        active = false;
    }

    public synchronized void ensureActive() {
        if(!active) throw new APIUnavailableException("The extension API is no longer available.");
    }

    public String getExtensionId() {
        return metadata.extId;
    }

    public ExtensionMetadata getExtensionMetadata() {
        return metadata;
    }

    public OPanel getPlugin() {
        return plugin;
    }

    public OPanelServer getServer() {
        ensureActive();
        OPanelServer server = plugin.getServer();
        if(server == null) throw new ServerUnavailableException("The server is unavailable.");
        return server;
    }

    public OPanelPlayer getPlayer(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        OPanelPlayer player = call("resolve player", () -> getServer().getPlayer(uniqueId.toString()));
        if(player == null) {
            throw new PlayerUnavailableException(uniqueId, "Player " + uniqueId + " is unavailable.");
        }
        return player;
    }

    public String getLogPrefix() {
        ensureActive();
        return "[" + metadata.name + "] ";
    }

    public <T> T call(String operation, ThrowingSupplier<T> supplier) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(supplier, "supplier");
        ensureActive();
        try {
            return supplier.get();
        } catch (ActLaterException | OPanelAPIException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailedException(operation, e);
        }
    }

    public void run(String operation, ThrowingRunnable runnable) {
        call(operation, () -> {
            runnable.run();
            return null;
        });
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static ExtensionAPI buildApi(OPanel plugin, ExtensionMetadata metadata) {
        return new ExtensionAPI(new ExtensionContext(plugin, metadata));
    }
}
