package net.opanel.bukkit_1_21_5;

import net.opanel.logger.Loggable;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class LoggerImpl implements Loggable {
    private final Logger logger;

    LoggerImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warning(msg);
    }

    @Override
    public void error(String msg) {
        logger.severe(msg);
    }

    /** @todo */
    @Override
    public List<String> getLogFileList() throws IOException {
        return List.of();
    }

    /** @todo */
    @Override
    public String getLogContent(String fileName) throws IOException {
        return "";
    }

    /** @todo */
    @Override
    public void deleteLog(String fileName) throws IOException {

    }
}
