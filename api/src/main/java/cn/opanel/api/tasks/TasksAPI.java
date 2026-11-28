package cn.opanel.api.tasks;

import cn.opanel.api.exception.OperationFailedException;

import java.util.List;
import java.util.Optional;

/**
 * Management API for OPanel's persistent command scheduler.
 *
 * <p>Task schedules use five-field UNIX cron expressions. Command lists accept
 * normal server commands and OPanel's task-control syntax. Returned task objects
 * and lists are immutable snapshots; changes must be made through this API.</p>
 *
 * <p>OPanel 持久化命令调度器的管理 API。</p>
 *
 * <p>任务计划使用五字段 UNIX cron 表达式。命令列表接受普通服务器命令以及
 * OPanel 的任务控制语法。返回的任务对象与列表都是不可变快照；修改必须
 * 通过本 API 进行。</p>
 */
public interface TasksAPI { // OPanel 持久化命令调度器的管理 API
    /**
     * @return an unmodifiable snapshot of all scheduled tasks
     */
    List<ScheduledTaskInfo> getTasks(); // 返回所有定时任务的不可修改快照

    /**
     * Resolves a task by its stable ID.
     *
     * @param id non-blank task ID
     * @return the task snapshot, or an empty value when no task has that ID
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    Optional<ScheduledTaskInfo> getTask(String id); // 按稳定 ID 解析任务

    /**
     * Creates and schedules a task. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Newly created tasks are enabled by default.
     *
     * @param name non-blank display name
     * @param cron non-blank five-field UNIX cron expression
     * @param commands non-empty ordered command list
     * @return an immutable snapshot containing the generated task ID
     * @throws NullPointerException if an argument or command entry is {@code null}
     * @throws IllegalArgumentException if a required value is blank/empty
     * @throws OperationFailedException if the cron
     *         expression or task command syntax is invalid
     */
    ScheduledTaskInfo createTask(String name, String cron, List<String> commands); // 创建并调度任务

    /**
     * Atomically validates and replaces a task definition. This operation may
     * block and must not be called from an extension lifecycle callback or the
     * Minecraft main thread. If validation fails, the existing task definition
     * and schedule remain unchanged. The enabled state is preserved.
     *
     * @param id non-blank ID of the task to update
     * @param name non-blank replacement display name
     * @param cron non-blank replacement five-field UNIX cron expression
     * @param commands non-empty replacement command list
     * @return an immutable snapshot of the updated task
     * @throws NullPointerException if an argument or command entry is {@code null}
     * @throws IllegalArgumentException if a required value is blank/empty
     * @throws OperationFailedException if the task does
     *         not exist or the replacement definition is invalid
     */
    ScheduledTaskInfo updateTask(String id, String name, String cron, List<String> commands); // 原子地校验并替换任务定义

    /**
     * Changes whether a task is enabled. This operation may block and must not be
     * called from an extension lifecycle callback or the Minecraft main thread.
     * Disabling a task preserves its definition and future schedule.
     *
     * @param id non-blank task ID
     * @param enabled {@code true} to execute future occurrences, {@code false} to skip them
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     * @throws OperationFailedException if the task does not exist
     */
    void setTaskEnabled(String id, boolean enabled); // 更改任务是否启用

    /**
     * Deletes a task. This operation may block and must not be called from an
     * extension lifecycle callback or the Minecraft main thread.
     * Any pending scheduled callback for the task is cancelled.
     *
     * @param id non-blank task ID
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     * @throws OperationFailedException if the task does not exist
     */
    void deleteTask(String id); // 删除任务
}
