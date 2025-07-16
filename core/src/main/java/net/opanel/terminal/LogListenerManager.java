package net.opanel.terminal;

import java.util.List;
import java.util.function.Consumer;

public interface LogListenerManager {
    void addListener(Consumer<ConsoleLog> listener);
    void clearListeners();
    List<ConsoleLog> getRecentLogs();
}
