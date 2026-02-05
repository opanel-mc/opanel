package net.opanel.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for backup storage providers.
 * Implementations handle uploading, listing, and deleting backups to/from
 * various storage backends.
 */
public interface BackupProvider {

    /**
     * Gets the type of this provider.
     */
    BackupProviderType getProviderType();

    /**
     * Uploads a backup file to the storage.
     *
     * @param zipFile  The path to the zip file to upload
     * @param fileName The name to use for the backup file in storage
     * @throws IOException if the upload fails
     */
    void upload(Path zipFile, String fileName) throws IOException;

    /**
     * Downloads a backup file from the storage.
     *
     * @param fileName The name of the backup file to download
     * @param targetPath The path to write the downloaded file
     * @throws IOException if the download fails
     */
    void download(String fileName, Path targetPath) throws IOException;

    /**
     * Deletes a backup file from the storage.
     *
     * @param fileName The name of the backup file to delete
     * @throws IOException if the deletion fails
     */
    void delete(String fileName) throws IOException;

    /**
     * Lists all backup files in the storage.
     *
     * @return List of backup information, sorted by timestamp descending (newest
     *         first)
     * @throws IOException if listing fails
     */
    List<BackupInfo> list() throws IOException;

    /**
     * Checks if the provider is properly configured and ready to use.
     *
     * @return true if the provider is configured
     */
    boolean isConfigured();
}
