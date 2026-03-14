package net.opanel.backup.provider;

import java.nio.file.Path;
import java.util.Map;

public interface BackupProvider {
    void testConnection() throws Exception;
    void upload(Path sourcePath, String remoteKey, Map<String, String> metadata) throws Exception;
    void download(String remoteKey, Path targetPath) throws Exception;
    void delete(String remoteKey) throws Exception;
}
