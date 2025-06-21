package net.opanel.bukkit_1_21_5;

import net.opanel.logger.Loggable;

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
}
