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
 */
public interface TasksAPI {
    /**
     * @return an unmodifiable snapshot of all scheduled tasks
     */
    List<ScheduledTaskInfo> getTasks();

    /**
     * Resolves a task by its stable ID.
     *
     * @param id non-blank task ID
     * @return the task snapshot, or an empty value when no task has that ID
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    Optional<ScheduledTaskInfo> getTask(String id);

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
    ScheduledTaskInfo createTask(String name, String cron, List<String> commands);

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
    ScheduledTaskInfo updateTask(String id, String name, String cron, List<String> commands);

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
    void setTaskEnabled(String id, boolean enabled);

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
    void deleteTask(String id);
}
