package net.opanel.spigot_1_20_5;

import net.opanel.logger.Loggable;

import java.util.logging.Logger;

public class LoggerImpl extends Loggable {
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
