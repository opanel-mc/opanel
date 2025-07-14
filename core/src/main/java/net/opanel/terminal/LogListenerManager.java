package net.opanel.terminal;

import java.util.List;
import java.util.function.Consumer;

public interface LogListenerManager {
    void addListener(Consumer<String> listener);
    void clearListeners();
    List<String> getRecentLogs();
}
