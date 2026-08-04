package net.opanel.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.opanel.common.ServerType;
import net.opanel.exception.ActLaterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PluginUpdateManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsAndMapsTheModrinthBulkHashContract() {
        List<String> hashes = Arrays.asList("hash-a", "hash-missing", "hash-b");
        JsonObject request = PluginUpdateManager.buildVersionFilesRequest(hashes);
        JsonObject response = new JsonObject();
        JsonObject versionA = new JsonObject();
        versionA.addProperty("id", "version-a");
        response.add("hash-a", versionA);
        JsonObject versionB = new JsonObject();
        versionB.addProperty("id", "version-b");
        response.add("hash-b", versionB);

        List<JsonObject> versions = PluginUpdateManager.mapVersionsByHash(hashes, response);

        assertEquals("sha1", request.get("algorithm").getAsString());
        assertEquals(3, request.getAsJsonArray("hashes").size());
        assertEquals("version-a", versions.get(0).get("id").getAsString());
        assertNull(versions.get(1));
        assertEquals("version-b", versions.get(2).get("id").getAsString());
    }

    @Test
    void selectsOnlyVersionsForTheCurrentServerPlatform() {
        JsonObject installed = version("installed", "2026-01-01T00:00:00Z", "release", "paper", "1.21.1");
        JsonArray versions = new JsonArray();
        versions.add(version("forge-newest", "2026-03-01T00:00:00Z", "release", "forge", "1.21.1"));
        versions.add(version("paper-newer", "2026-02-01T00:00:00Z", "release", "paper", "1.21.1"));

        JsonObject selected = PluginUpdateManager.pickTarget(
            versions,
            "1.21.1",
            "1.21",
            ServerType.PAPER,
            installed
        );

        assertNotNull(selected);
        assertEquals("paper-newer", selected.get("id").getAsString());
    }

    @Test
    void keepsReleaseUsersOnTheReleaseChannel() {
        JsonObject installed = version("installed", "2026-01-01T00:00:00Z", "release", "fabric", "1.21.1");
        JsonArray versions = new JsonArray();
        versions.add(version("beta-newest", "2026-03-01T00:00:00Z", "beta", "fabric", "1.21.1"));
        versions.add(version("release-newer", "2026-02-01T00:00:00Z", "release", "fabric", "1.21.1"));

        JsonObject selected = PluginUpdateManager.pickTarget(
            versions,
            "1.21.1",
            "1.21",
            ServerType.FABRIC,
            installed
        );

        assertNotNull(selected);
        assertEquals("release-newer", selected.get("id").getAsString());
    }

    @Test
    void comparesPublicationTimeInsteadOfArbitraryVersionNumbers() {
        JsonObject installed = version("installed", "2026-01-01T00:00:00Z", "release", "fabric", "1.21.1");
        installed.addProperty("version_number", "build-999999999999999999999999");
        JsonObject target = version("target", "2026-02-01T00:00:00Z", "release", "fabric", "1.21.1");
        target.addProperty("version_number", "release-1");

        assertTrue(PluginUpdateManager.isPublishedAfter(installed, target));
    }

    @Test
    void continuesApplyingAfterAnUpdateIsDeferred() throws IOException {
        List<PluginUpdate> updates = Arrays.asList(update("first.jar"), update("second.jar"));
        List<Path> files = Arrays.asList(Path.of("first.tmp"), Path.of("second.tmp"));
        AtomicInteger applied = new AtomicInteger();

        boolean deferred = PluginUpdateManager.applyDownloadedUpdates(updates, files, (fileName, path) -> {
            applied.incrementAndGet();
            if("first.jar".equals(fileName)) throw new ActLaterException();
        });

        assertTrue(deferred);
        assertEquals(2, applied.get());
    }

    @Test
    void replacesAStagedLegacyUpdate() throws IOException {
        Path source = tempDir.resolve("plugin.jar.update");
        Path target = tempDir.resolve("plugin.jar");
        Files.writeString(source, "new");
        Files.writeString(target, "old");

        assertTrue(DeferredFileReplacer.replaceWithRetries(source, target));
        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(source));
    }

    private static JsonObject version(String id, String date, String channel, String loader, String gameVersion) {
        JsonObject version = new JsonObject();
        version.addProperty("id", id);
        version.addProperty("date_published", date);
        version.addProperty("version_type", channel);
        JsonArray loaders = new JsonArray();
        loaders.add(loader);
        version.add("loaders", loaders);
        JsonArray gameVersions = new JsonArray();
        gameVersions.add(gameVersion);
        version.add("game_versions", gameVersions);
        return version;
    }

    private static PluginUpdate update(String fileName) {
        return new PluginUpdate(
            fileName,
            fileName,
            "1",
            "2",
            "https://example.invalid/" + fileName,
            null,
            "new-sha1",
            "old-sha1"
        );
    }
}
