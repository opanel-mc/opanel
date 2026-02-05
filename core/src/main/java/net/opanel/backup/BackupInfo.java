package net.opanel.backup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information about a backup file.
 */
public class BackupInfo {
    private static final Pattern BACKUP_NAME_PATTERN = Pattern.compile(
            "backup-(.+)-(\\d{4}-\\d{2}-\\d{2}-\\d{6})\\.zip");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private final String fileName;
    private final long size;
    private final LocalDateTime timestamp;
    private final String saveName;

    public BackupInfo(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;

        // Parse timestamp and save name from filename
        Matcher matcher = BACKUP_NAME_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            this.saveName = matcher.group(1);
            this.timestamp = LocalDateTime.parse(matcher.group(2), TIMESTAMP_FORMAT);
        } else {
            this.saveName = "";
            this.timestamp = null;
        }
    }

    public BackupInfo(String fileName, long size, LocalDateTime timestamp, String saveName) {
        this.fileName = fileName;
        this.size = size;
        this.timestamp = timestamp;
        this.saveName = saveName;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSaveName() {
        return saveName;
    }

    /**
     * Generates a backup filename based on save name and current time.
     *
     * @param saveName The name of the save/world being backed up
     * @return The generated filename
     */
    public static String generateFileName(String saveName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return "backup-" + saveName + "-" + timestamp + ".zip";
    }
}
