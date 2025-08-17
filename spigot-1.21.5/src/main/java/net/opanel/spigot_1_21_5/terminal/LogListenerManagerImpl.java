package net.opanel.spigot_1_21_5.terminal;

import net.opanel.terminal.ConsoleLog;
import net.opanel.terminal.LogListenerManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogListenerManagerImpl extends Handler implements LogListenerManager {
    private final List<ConsoleLog> logs = new ArrayList<>();
    private final Set<Consumer<ConsoleLog>> listeners = new HashSet<>();

    @Override
    public void publish(LogRecord record) {
        if(record.getLevel() != Level.INFO && record.getLevel() != Level.WARNING && record.getLevel() != Level.SEVERE) return;

        final long time = record.getMillis();
        String level = record.getLevel().getName();
        if(level.equals("SEVERE")) level = "ERROR";
        final String thread = getThreadName(record.getLongThreadID());
        final String source = record.getLoggerName();
        final String line = record.getMessage();
        final ConsoleLog log = new ConsoleLog(time, level, thread, source, line);

        logs.add(log);
        listeners.forEach(listener -> {
            listener.accept(log);
        });
    }

    private String getThreadName(long threadID) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for(Thread thread : threads) {
            if(thread.threadId() == threadID) {
                return thread.getName();
            }
        }
        return null;
    }

    @Override
    public void flush() { }

    @Override
    public void close() throws SecurityException {
        //clearListeners();
    }

    @Override
    public void addListener(Consumer<ConsoleLog> listener) {
        listeners.add(listener);
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public List<ConsoleLog> getRecentLogs() {
        return logs;
    }
}
