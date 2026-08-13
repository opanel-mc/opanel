package net.opanel.task;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.exception.IllegalTaskCommandSyntaxException;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScheduledTaskManager {
    private final OPanel plugin;
    private final List<ScheduledTask> tasks;
    private final Map<String, TaskFutureRef> taskFutureRefs = new HashMap<>();
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );
    private final CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    public static final List<ScheduledTask> DEFAULT_TASKS = List.of(
        new ScheduledTask("restart-server", "定时重启服务器", "0 0 * * *", List.of("@restart"), false)
    );

    /** Future reference and identity token for one cron configuration */
    private static class TaskFutureRef {
        private ScheduledFuture<?> future;
    }

    @SuppressWarnings("unchecked")
    public ScheduledTaskManager(OPanel plugin) {
        this.plugin = plugin;
        tasks = new CopyOnWriteArrayList<>(
            (List<ScheduledTask>) Storage.get().getStoredData(StorageKey.SCHEDULED_TASKS)
        );

        writeLock.lock();
        try {
            for(ScheduledTask task : tasks) {
                scheduleTask(ExecutionTime.forCron(cronParser.parse(task.getCron())), task);
            }
        } finally {
            writeLock.unlock();
        }
    }

    // Should be called within write lock
    private void saveTasks() {
        Storage.get().setStoredData(StorageKey.SCHEDULED_TASKS, new ArrayList<>(tasks));
    }

    // Should be called within write lock
    private void scheduleTask(ExecutionTime executionTime, ScheduledTask task) {
        TaskFutureRef existingFutureRef = taskFutureRefs.get(task.getId());
        if(existingFutureRef != null && existingFutureRef.future != null && !existingFutureRef.future.isDone()) {
            existingFutureRef.future.cancel(false);
        }

        TaskFutureRef futureRef = new TaskFutureRef();
        taskFutureRefs.put(task.getId(), futureRef);
        scheduleTask(executionTime, task, ZonedDateTime.now(), futureRef);
    }

    // Should be called within write lock
    private void scheduleTask(ExecutionTime executionTime, ScheduledTask task, ZonedDateTime after, TaskFutureRef futureRef) {
        // Prevent a stale callback from rescheduling a task
        // after its future reference was replaced or removed
        if(taskFutureRefs.get(task.getId()) != futureRef) return;

        Optional<ZonedDateTime> nextOptional = executionTime.nextExecution(after);
        if(nextOptional.isEmpty()) return;

        ZonedDateTime next = nextOptional.get();
        long timeout = Math.max(0, Duration.between(ZonedDateTime.now(), next).toNanos());
        
        futureRef.future = executor.schedule(() -> {
            OPanelServer server = plugin.getServer();
            if(server == null) return;

            readLock.lock();
            try {
                // Prevent an already-started callback from executing
                // after its future reference was replaced or removed
                if(taskFutureRefs.get(task.getId()) != futureRef) return;

                if(task.isEnabled()) {
                    List<String> commands = new ArrayList<>(task.getCommands());

                    TaskCommandExecutor.execute(server, commands);
                }
            } finally {
                readLock.unlock();
            }

            ZonedDateTime rescheduleAfter = ZonedDateTime.now();
            // Prevent an early callback or clock rollback from scheduling the same occurrence again.
            if(rescheduleAfter.isBefore(next)) {
                rescheduleAfter = next;
            }

            writeLock.lock();
            try {
                scheduleTask(executionTime, task, rescheduleAfter, futureRef);
            } finally {
                writeLock.unlock();
            }
        }, timeout, TimeUnit.NANOSECONDS);
    }

    public ScheduledTask createTask(String name, String cronExpression, List<String> commands) throws IllegalArgumentException, IllegalTaskCommandSyntaxException {
        writeLock.lock();
        try {
            TaskCommandParser.parse(commands); // parse commands first to catch the syntax error if it has
            Cron cron = cronParser.parse(cronExpression); // parse cron first to catch the syntax error if it has
            ScheduledTask task = new ScheduledTask(
                Utils.generateRandomCharSequence(16, false),
                name,
                cronExpression,
                new ArrayList<>(commands),
                true
            );
            tasks.add(task);
            scheduleTask(ExecutionTime.forCron(cron), task);
            saveTasks();
            return task;
        } finally {
            writeLock.unlock();
        }
    }

    public void deleteTask(String id) {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: " + id);
            }

            TaskFutureRef futureRef = taskFutureRefs.remove(id);
            if(futureRef != null && futureRef.future != null && !futureRef.future.isDone()) {
                futureRef.future.cancel(false);
            }

            tasks.remove(task);
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }

    public List<ScheduledTask> getTasks() {
        readLock.lock();
        try {
            return new ArrayList<>(tasks);
        } finally {
            readLock.unlock();
        }
    }

    public ScheduledTask getTask(String id) {
        readLock.lock();
        try {
            return getTaskUnsafe(id);
        } finally {
            readLock.unlock();
        }
    }

    public ScheduledTask updateTask(
            String id,
            String name,
            String cronExpression,
            List<String> commands
    ) throws IllegalArgumentException, IllegalTaskCommandSyntaxException {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: " + id);
            }

            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(commands, "commands");
            TaskCommandParser.parse(commands);
            Cron cron = cronParser.parse(cronExpression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            List<String> commandSnapshot = new ArrayList<>(commands);

            task.setName(name);
            task.setCron(cronExpression);
            task.setCommands(commandSnapshot);
            scheduleTask(executionTime, task);
            saveTasks();
            return task;
        } finally {
            writeLock.unlock();
        }
    }

    private ScheduledTask getTaskUnsafe(String id) {
        for(ScheduledTask task : tasks) {
            if(task.getId().equals(id)) {
                return task;
            }
        }
        return null;
    }

    public void setTaskName(String id, String name) {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: "+ id);
            }

            task.setName(name);
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }

    public void setTaskCron(String id, String cronExpression) throws IllegalArgumentException {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: "+ id);
            }

            Cron cron = cronParser.parse(cronExpression); // parse cron first to catch the syntax error if it has
            task.setCron(cronExpression);
            scheduleTask(ExecutionTime.forCron(cron), task);
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }

    public void setTaskCommands(String id, List<String> commands) throws IllegalTaskCommandSyntaxException {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: "+ id);
            }

            TaskCommandParser.parse(commands); // parse commands first to catch syntax error if it has
            task.setCommands(new ArrayList<>(commands));
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }

    public void setTaskEnabled(String id, boolean enabled) {
        writeLock.lock();
        try {
            ScheduledTask task = getTaskUnsafe(id);
            if(task == null) {
                throw new NoSuchElementException("Cannot find the task: "+ id);
            }

            task.setEnabled(enabled);
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }

    public void shutdown() {
        writeLock.lock();
        try {
            for(TaskFutureRef futureRef : taskFutureRefs.values()) {
                if(futureRef.future != null && !futureRef.future.isDone()) {
                    futureRef.future.cancel(false);
                }
            }
            taskFutureRefs.clear();
            executor.shutdownNow();
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }
}
