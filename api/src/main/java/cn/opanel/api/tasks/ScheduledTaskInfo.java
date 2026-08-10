package cn.opanel.api.tasks;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of one persistent scheduled task.
 */
public final class ScheduledTaskInfo {
    private final String id;
    private final String name;
    private final String cron;
    private final List<String> commands;
    private final boolean enabled;

    /**
     * Creates a scheduled-task snapshot.
     *
     * @param id stable task ID
     * @param name task display name
     * @param cron five-field UNIX cron expression
     * @param commands ordered command list
     * @param enabled whether future occurrences should execute
     * @throws NullPointerException if an argument or command entry is {@code null}
     */
    public ScheduledTaskInfo(String id, String name, String cron, List<String> commands, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.cron = Objects.requireNonNull(cron, "cron");
        this.commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        this.enabled = enabled;
    }

    /**
     * @return the stable task ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return the task display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the five-field UNIX cron expression
     */
    public String getCron() {
        return cron;
    }

    /**
     * @return an unmodifiable ordered command list
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * @return {@code true} when future task occurrences should execute
     */
    public boolean isEnabled() {
        return enabled;
    }
}
