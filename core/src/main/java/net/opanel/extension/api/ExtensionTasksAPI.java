package net.opanel.extension.api;

import net.opanel.api.tasks.ScheduledTaskInfo;
import net.opanel.api.tasks.TasksAPI;
import net.opanel.extension.ExtensionContext;
import net.opanel.task.ScheduledTask;
import net.opanel.task.ScheduledTaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ExtensionTasksAPI implements TasksAPI {
    private final ExtensionContext ctx;

    ExtensionTasksAPI(ExtensionContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    @Override
    public List<ScheduledTaskInfo> getTasks() {
        return ctx.call("get scheduled tasks", () -> {
            List<ScheduledTaskInfo> tasks = new ArrayList<>();
            for(ScheduledTask task : manager().getTasks()) {
                tasks.add(toTaskInfo(task));
            }
            return Collections.unmodifiableList(tasks);
        });
    }

    @Override
    public Optional<ScheduledTaskInfo> getTask(String id) {
        validateId(id);
        return ctx.call("get scheduled task", () -> {
            ScheduledTask task = manager().getTask(id);
            return task == null ? Optional.empty() : Optional.of(toTaskInfo(task));
        });
    }

    @Override
    public ScheduledTaskInfo createTask(String name, String cron, List<String> commands) {
        List<String> commandSnapshot = validateTaskDefinition(name, cron, commands);
        return ctx.call("create scheduled task", () -> toTaskInfo(
                manager().createTask(name, cron, commandSnapshot)
        ));
    }

    @Override
    public ScheduledTaskInfo updateTask(String id, String name, String cron, List<String> commands) {
        validateId(id);
        List<String> commandSnapshot = validateTaskDefinition(name, cron, commands);
        return ctx.call("update scheduled task", () -> toTaskInfo(
                manager().updateTask(id, name, cron, commandSnapshot)
        ));
    }

    @Override
    public void setTaskEnabled(String id, boolean enabled) {
        validateId(id);
        ctx.run("set scheduled task enabled status", () -> manager().setTaskEnabled(id, enabled));
    }

    @Override
    public void deleteTask(String id) {
        validateId(id);
        ctx.run("delete scheduled task", () -> manager().deleteTask(id));
    }

    private ScheduledTaskManager manager() {
        ctx.ensureActive();
        return ctx.getPlugin().getScheduledTaskManager();
    }

    private static void validateId(String id) {
        Objects.requireNonNull(id, "id");
        if(id.isBlank()) throw new IllegalArgumentException("id must not be blank");
    }

    private static List<String> validateTaskDefinition(String name, String cron, List<String> commands) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(cron, "cron");
        Objects.requireNonNull(commands, "commands");
        if(name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if(cron.isBlank()) throw new IllegalArgumentException("cron must not be blank");
        if(commands.isEmpty()) throw new IllegalArgumentException("commands must not be empty");

        List<String> commandSnapshot = new ArrayList<>(commands.size());
        for(String command : commands) {
            commandSnapshot.add(Objects.requireNonNull(command, "command"));
        }
        return commandSnapshot;
    }

    private static ScheduledTaskInfo toTaskInfo(ScheduledTask task) {
        return new ScheduledTaskInfo(
                task.getId(),
                task.getName(),
                task.getCron(),
                task.getCommands(),
                task.isEnabled()
        );
    }
}
