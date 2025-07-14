package net.opanel.fabric_1_21_5.terminal;

import net.opanel.terminal.LogListenerManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Plugin(name = "LogListenerAppender", category = "Core", elementType = "appender")
public class LogListenerManagerImpl extends AbstractAppender implements LogListenerManager {
    private final List<String> logs = new ArrayList<>();
    private final Set<Consumer<String>> listeners = new HashSet<>();

    protected LogListenerManagerImpl(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent e) {
        if(e.getLevel() != Level.INFO) return;

        final String line = e.getMessage().getFormattedMessage();
        logs.add(line);
        listeners.forEach(listener -> {
             listener.accept(line);
        });
    }

    @PluginFactory
    public static LogListenerManagerImpl createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions
    ) {
        PatternLayout pattern = PatternLayout.createDefaultLayout();
        return new LogListenerManagerImpl(name, null, pattern, ignoreExceptions, null);
    }

    @Override
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public List<String> getRecentLogs() {
        return logs;
    }
}
