package net.opanel.monitor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Called on the existing monitor worker, never on the game thread. */
final class PerformanceIncidentStore {
    private static final Gson GSON = new Gson();
    private final Path path;
    private long lastWrite;
    PerformanceIncidentStore(Path path) { this.path = path; }
    void load() throws java.io.IOException {
        if(!Files.exists(path)) return;
        if(Files.size(path) > 16 * 1024 * 1024) throw new java.io.IOException("Incident file exceeds size limit");
        try {
            List<PerformanceIncidentRecorder.Incident> records = GSON.fromJson(Files.readString(path),
                    new TypeToken<List<PerformanceIncidentRecorder.Incident>>() {}.getType());
            if(records != null) PerformanceTickSampler.restore(records);
        } catch(RuntimeException e) { throw new java.io.IOException("Invalid incident file", e); }
    }
    void save(boolean force) throws java.io.IOException {
        if(!PerformanceTickSampler.enabled()) return;
        long now = System.currentTimeMillis();
        if(!force && now - lastWrite < 30_000) return;
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(PerformanceTickSampler.incidents()));
        try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch(java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        lastWrite = now;
    }
}
