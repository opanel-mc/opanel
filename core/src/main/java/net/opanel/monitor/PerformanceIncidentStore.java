package net.opanel.monitor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** Called on the existing monitor worker, never on the game thread. */
final class PerformanceIncidentStore {
    private static final Gson GSON = new Gson();
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path path;
    private long lastWrite;
    private boolean loadFailed;
    PerformanceIncidentStore(Path path) { this.path = path; }
    void load() throws java.io.IOException {
        if(!Files.exists(path)) return;
        try {
            if(Files.size(path) > MAX_BYTES) throw new java.io.IOException("Incident file exceeds size limit");
            List<PerformanceIncidentRecorder.Incident> records = GSON.fromJson(Files.readString(path),
                    new TypeToken<List<PerformanceIncidentRecorder.Incident>>() {}.getType());
            if(records != null) PerformanceTickSampler.restore(records);
        } catch(RuntimeException e) {
            loadFailed = true;
            throw new java.io.IOException("Invalid incident file", e);
        } catch(java.io.IOException e) {
            loadFailed = true;
            throw e;
        }
    }
    void save(boolean force) throws java.io.IOException {
        if(!PerformanceTickSampler.enabled()) return;
        if(loadFailed) throw new java.io.IOException("Incident file was not loaded; refusing to overwrite it");
        long now = System.currentTimeMillis();
        if(!force && now - lastWrite < 30_000) return;
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        var bytes = new ByteArrayOutputStream();
        bytes.write('[');
        boolean first = true;
        for(var incident : PerformanceTickSampler.incidents()) {
            byte[] entry = GSON.toJson(incident).getBytes(StandardCharsets.UTF_8);
            if((long) bytes.size() + entry.length + (first ? 0 : 1) + 1 > MAX_BYTES) {
                if(first) throw new java.io.IOException("A single incident exceeds the size limit");
                break;
            }
            if(!first) bytes.write(',');
            bytes.write(entry);
            first = false;
        }
        bytes.write(']');
        Files.write(temporary, bytes.toByteArray());
        try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch(java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        lastWrite = now;
    }
}
