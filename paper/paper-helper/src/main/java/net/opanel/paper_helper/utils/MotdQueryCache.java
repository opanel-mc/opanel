package net.opanel.paper_helper.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class MotdQueryCache {
    private static final int QUERY_TIMEOUT_MILLIS = 1500;
    private static final int MAX_RESPONSE_LENGTH = 1024 * 1024;
    private static final long CACHE_TTL_MILLIS = 5000L;
    private static final String IPV4_LOOPBACK_ADDRESS = "127.0.0.1";
    private static final String IPV6_LOOPBACK_ADDRESS = "::1";
    private static final Gson GSON = new Gson();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final JavaPlugin plugin;
    private final Server server;
    private final AtomicReference<CompletableFuture<Snapshot>> pendingRefresh = new AtomicReference<>();
    private volatile Snapshot snapshot;
    private volatile long lastAttemptAt;

    public MotdQueryCache(JavaPlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
        refreshAsync(server.getMotd(), true);
    }

    public CompletableFuture<String> getMotdAsync() {
        String baseMotd = server.getMotd();
        Snapshot current = snapshot;
        boolean baseMotdChanged = current != null && !Objects.equals(current.baseMotd(), baseMotd);

        if(current != null && !baseMotdChanged && !isExpired(current)) {
            return CompletableFuture.completedFuture(current.effectiveMotd());
        }

        return refreshAsync(baseMotd, baseMotdChanged).handle((result, error) -> {
            String currentBaseMotd = server.getMotd();
            Snapshot latest = snapshot;
            if(latest != null && Objects.equals(latest.baseMotd(), currentBaseMotd)) {
                return latest.effectiveMotd();
            }
            return currentBaseMotd;
        });
    }

    private boolean isExpired(Snapshot current) {
        return System.currentTimeMillis() - current.queriedAt() >= CACHE_TTL_MILLIS;
    }

    private CompletableFuture<Snapshot> refreshAsync(String baseMotd, boolean force) {
        CompletableFuture<Snapshot> currentRefresh = pendingRefresh.get();
        if(currentRefresh != null) return currentRefresh;

        long now = System.currentTimeMillis();
        if(!force && now - lastAttemptAt < CACHE_TTL_MILLIS) {
            return CompletableFuture.completedFuture(snapshot);
        }

        CompletableFuture<Snapshot> newRefresh;
        while(true) {
            currentRefresh = pendingRefresh.get();
            if(currentRefresh != null) return currentRefresh;

            newRefresh = new CompletableFuture<>();
            if(pendingRefresh.compareAndSet(null, newRefresh)) break;
        }

        lastAttemptAt = now;
        CompletableFuture<Snapshot> scheduledRefresh = newRefresh;
        try {
            CompletableFuture.runAsync(() -> refresh(baseMotd, scheduledRefresh));
        } catch (RuntimeException e) {
            pendingRefresh.compareAndSet(scheduledRefresh, null);
            scheduledRefresh.completeExceptionally(e);
            plugin.getLogger().fine("Failed to schedule MOTD query: "+ e.getMessage());
        }
        return scheduledRefresh;
    }

    private void refresh(String baseMotd, CompletableFuture<Snapshot> refreshFuture) {
        try {
            String effectiveMotd = queryMotd(getConnectHost(), server.getPort(), getProtocolVersion());
            Snapshot newSnapshot = new Snapshot(baseMotd, effectiveMotd, System.currentTimeMillis());
            snapshot = newSnapshot;
            refreshFuture.complete(newSnapshot);
        } catch (Exception | LinkageError e) {
            refreshFuture.completeExceptionally(e);
            plugin.getLogger().fine("Failed to query effective MOTD: "+ e.getMessage());
        } finally {
            pendingRefresh.compareAndSet(refreshFuture, null);

            String currentBaseMotd = server.getMotd();
            if(!Objects.equals(currentBaseMotd, baseMotd)) {
                refreshAsync(currentBaseMotd, true);
            }
        }
    }

    private String getConnectHost() {
        String serverIp = server.getIp();
        if(serverIp == null || serverIp.isBlank() || serverIp.equals("0.0.0.0")) {
            return IPV4_LOOPBACK_ADDRESS;
        }
        if(serverIp.equals("::") || serverIp.equals("0:0:0:0:0:0:0:0")) {
            return IPV6_LOOPBACK_ADDRESS;
        }
        return serverIp;
    }

    private int getProtocolVersion() {
        try {
            Object unsafeValues = Bukkit.getUnsafe();
            return (int) unsafeValues.getClass().getMethod("getProtocolVersion").invoke(unsafeValues);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return -1;
        }
    }

    private static String queryMotd(String host, int port, int protocolVersion) throws IOException {
        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), QUERY_TIMEOUT_MILLIS);
            socket.setSoTimeout(QUERY_TIMEOUT_MILLIS);

            try(
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream())
            ) {
                writeHandshake(output, host, port, protocolVersion);
                writeStatusRequest(output);

                int packetLength = readVarInt(input);
                if(packetLength <= 0 || packetLength > MAX_RESPONSE_LENGTH) {
                    throw new IOException("Invalid status response length: "+ packetLength);
                }

                int packetId = readVarInt(input);
                if(packetId != 0) {
                    throw new IOException("Unexpected status response packet id: "+ packetId);
                }

                int jsonLength = readVarInt(input);
                if(jsonLength <= 0 || jsonLength > packetLength || jsonLength > MAX_RESPONSE_LENGTH) {
                    throw new IOException("Invalid status JSON length: "+ jsonLength);
                }

                byte[] jsonBytes = input.readNBytes(jsonLength);
                if(jsonBytes.length != jsonLength) throw new EOFException("Incomplete status response");

                return parseMotd(new String(jsonBytes, StandardCharsets.UTF_8));
            }
        }
    }

    private static void writeHandshake(DataOutputStream output, String host, int port, int protocolVersion) throws IOException {
        ByteArrayOutputStream packetBytes = new ByteArrayOutputStream();
        try(DataOutputStream packet = new DataOutputStream(packetBytes)) {
            writeVarInt(packet, 0);
            writeVarInt(packet, protocolVersion);
            writeString(packet, host);
            packet.writeShort(port);
            writeVarInt(packet, 1);
        }

        writeVarInt(output, packetBytes.size());
        output.write(packetBytes.toByteArray());
        output.flush();
    }

    private static void writeStatusRequest(DataOutputStream output) throws IOException {
        writeVarInt(output, 1);
        writeVarInt(output, 0);
        output.flush();
    }

    private static String parseMotd(String responseJson) throws IOException {
        JsonObject response;
        try {
            response = GSON.fromJson(responseJson, JsonObject.class);
        } catch (RuntimeException e) {
            throw new IOException("Invalid status response JSON", e);
        }
        if(response == null) throw new IOException("Invalid status response JSON");

        JsonElement description = response.get("description");
        if(description == null || description.isJsonNull()) {
            throw new IOException("Status response does not contain a description");
        }

        try {
            Component component = GsonComponentSerializer.gson().deserialize(description.toString());
            return LEGACY_SERIALIZER.serialize(component);
        } catch (RuntimeException e) {
            throw new IOException("Invalid status response description", e);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        do {
            int part = value & 0x7F;
            value >>>= 7;
            if(value != 0) part |= 0x80;
            output.writeByte(part);
        } while(value != 0);
    }

    private static int readVarInt(DataInputStream input) throws IOException {
        int result = 0;
        int bytesRead = 0;

        while(true) {
            byte current = input.readByte();
            result |= (current & 0x7F) << (bytesRead * 7);
            bytesRead++;

            if(bytesRead > 5) throw new IOException("VarInt is too large");
            if((current & 0x80) == 0) return result;
        }
    }

    private record Snapshot(String baseMotd, String effectiveMotd, long queriedAt) {}
}
