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
 *
 * <p>访问 Minecraft 服务器的日志文件。</p>
 *
 * <p>所有方法都在服务器日志目录内操作，且只接受安全的单段文件名。
 * 压缩的 GZIP 日志会由 {@link #getLogContent(String)} 透明地解压。
 * 返回的列表是不可变的快照，且不保证顺序。</p>
 */
public interface LogsAPI { // 服务器日志文件管理 API
    /**
     * Lists regular files in the server log directory.
     *
     * @return an unmodifiable snapshot of log file names
     */
    List<String> getLogFileList(); // 列出服务器日志目录中的常规文件，返回不可修改的日志文件名快照

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
    String getLogContent(String fileName); // 以 UTF-8 读取文本日志，.gz 文件会先解压再返回内容

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
    void deleteLog(String fileName); // 删除归档日志；活动的 .log 文件不可删除，且该操作可能阻塞
}
