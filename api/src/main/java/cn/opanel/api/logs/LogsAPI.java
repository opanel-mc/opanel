package cn.opanel.api.logs;

import cn.opanel.api.exception.OperationFailedException;

import java.util.List;

/**
 * Access to the Minecraft server's log files.
 *
 * <p>All methods operate inside the server's log directory and accept only a
 * safe, single-segment file name. Compressed GZIP logs are decompressed
 * transparently by {@link #getLogContent(String)}. Returned lists are immutable
 * snapshots and have no guaranteed ordering.</p>
 */
public interface LogsAPI {
    /**
     * Lists regular files in the server log directory.
     *
     * @return an unmodifiable snapshot of log file names
     */
    List<String> getLogFileList();

    /**
     * Reads a text log in UTF-8. Files ending in {@code .gz} are decompressed
     * before their content is returned.
     *
     * @param fileName safe file name relative to the log directory
     * @return decoded log content
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is unsafe
     * @throws OperationFailedException if the file does
     *         not exist, cannot be read, or has an unsupported file extension
     */
    String getLogContent(String fileName);

    /**
     * Deletes an archived log. Active .log files cannot be deleted. This operation
     * may block and must not be called from an extension lifecycle callback or the
     * Minecraft main thread.
     *
     * @param fileName archived log file to delete
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @throws IllegalArgumentException if the name is unsafe or identifies an
     *         active {@code .log} file
     * @throws OperationFailedException if the file does
     *         not exist or cannot be deleted
     */
    void deleteLog(String fileName);
}
