package cn.opanel.api.tasks;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of one persistent scheduled task.
 *
 * <p>单个持久化定时任务的不可变快照。</p>
 */
public final class ScheduledTaskInfo { // 单个持久化定时任务的不可变快照
    private final String id; // 稳定的任务 ID
    private final String name; // 任务显示名称
    private final String cron; // 五字段 UNIX cron 表达式
    private final List<String> commands; // 有序命令列表（不可修改）
    private final boolean enabled; // 未来的触发是否应执行

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
    public ScheduledTaskInfo(String id, String name, String cron, List<String> commands, boolean enabled) { // 创建定时任务快照
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.cron = Objects.requireNonNull(cron, "cron");
        this.commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        this.enabled = enabled;
    }

    /**
     * @return the stable task ID
     */
    public String getId() { // 返回稳定的任务 ID
        return id;
    }

    /**
     * @return the task display name
     */
    public String getName() { // 返回任务显示名称
        return name;
    }

    /**
     * @return the five-field UNIX cron expression
     */
    public String getCron() { // 返回五字段 UNIX cron 表达式
        return cron;
    }

    /**
     * @return an unmodifiable ordered command list
     */
    public List<String> getCommands() { // 返回不可修改的有序命令列表
        return commands;
    }

    /**
     * @return {@code true} when future task occurrences should execute
     */
    public boolean isEnabled() { // 未来的任务触发是否应执行
        return enabled;
    }
}
