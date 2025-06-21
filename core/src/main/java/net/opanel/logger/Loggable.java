package net.opanel.logger;

public interface Loggable {
    void info(String msg);
    void warn(String msg);
    void error(String msg);
}
