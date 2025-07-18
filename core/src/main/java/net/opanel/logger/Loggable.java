package net.opanel.logger;

import java.io.IOException;
import java.util.List;

public interface Loggable {
    void info(String msg);
    void warn(String msg);
    void error(String msg);
    List<String> getLogFileList() throws IOException;
    String getLogContent(String fileName) throws IOException;
    void deleteLog(String fileName) throws IOException;
}
