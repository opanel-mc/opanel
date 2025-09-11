package net.opanel.logger;

import net.opanel.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class Loggable {
    private static final Path logFolderPath = Paths.get("").resolve("logs");

    public abstract void info(String msg);
    public abstract void warn(String msg);
    public abstract void error(String msg);

    public List<String> getLogFileList() throws IOException {
        if(!Files.exists(logFolderPath)) {
            Files.createDirectory(logFolderPath);
        }
        if(!Files.isDirectory(logFolderPath)) {
            throw new IOException("Cannot find the logs folder, but found a logs file.");
        }

        List<String> fileList = new ArrayList<>();
        try(Stream<Path> stream = Files.list(logFolderPath)) {
            stream.filter(item -> !Files.isDirectory(item))
                    .map(Path::getFileName)
                    .forEach(name -> fileList.add(name.toString()));
        }
        return fileList;
    }

    public String getLogContent(String fileName) throws IOException {
        final Path filePath = Paths.get(logFolderPath.toString(), fileName);
        if(!Files.exists(filePath)) {
            throw new IOException("Cannot find the specified log file.");
        }
        if(filePath.toString().endsWith(".log") || filePath.toString().endsWith("txt")) {
            return Utils.readTextFile(filePath);
        }
        if(filePath.toString().endsWith(".gz")) {
            return Utils.decompressTextGzip(filePath);
        }
        throw new IOException("Unexpected file extension.");
    }

    public void deleteLog(String fileName) throws IOException {
        final Path filePath = Paths.get(logFolderPath.toString(), fileName);
        if(!Files.exists(filePath)) {
            throw new IOException("Cannot find the specified log file.");
        }
        Files.delete(filePath);
    }
}
