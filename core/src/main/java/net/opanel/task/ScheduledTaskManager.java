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
    private final Map<String, ScheduledFuture<?>> taskFutures = new ConcurrentHashMap<>();
    
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

    private void saveTasks() { // Should be called within write lock
        Storage.get().setStoredData(StorageKey.SCHEDULED_TASKS, new ArrayList<>(tasks));
    }

    private void scheduleTask(ExecutionTime executionTime, ScheduledTask task) {
        scheduleTask(executionTime, task, ZonedDateTime.now());
    }

    private void scheduleTask(ExecutionTime executionTime, ScheduledTask task, ZonedDateTime after) {
        ScheduledFuture<?> existingFuture = taskFutures.get(task.getId());
        if(existingFuture != null && !existingFuture.isDone()) {
            existingFuture.cancel(false);
        }

        Optional<ZonedDateTime> nextOptional = executionTime.nextExecution(after);
        if(nextOptional.isEmpty()) return;

        ZonedDateTime next = nextOptional.get();
        long timeout = Math.max(0, Duration.between(ZonedDateTime.now(), next).toNanos());
        
        ScheduledFuture<?> future = executor.schedule(() -> {
            OPanelServer server = plugin.getServer();
            if(server == null) return;

            if(task.isEnabled()) {
                readLock.lock();
                try {
                    List<String> commands = new ArrayList<>(task.getCommands());

                    TaskCommandExecutor.execute(server, commands);
                } finally {
                    readLock.unlock();
                }
            }

            ZonedDateTime rescheduleAfter = ZonedDateTime.now();
            // Prevent an early callback or clock rollback from scheduling the same occurrence again.
            if(rescheduleAfter.isBefore(next)) {
                rescheduleAfter = next;
            }
            scheduleTask(executionTime, task, rescheduleAfter);
        }, timeout, TimeUnit.NANOSECONDS);
        
        taskFutures.put(task.getId(), future);
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

            ScheduledFuture<?> future = taskFutures.remove(id);
            if(future != null && !future.isDone()) {
                future.cancel(false);
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
            for(ScheduledFuture<?> future : taskFutures.values()) {
                if(!future.isDone()) {
                    future.cancel(false);
                }
            }
            taskFutures.clear();
            executor.shutdownNow();
            saveTasks();
        } finally {
            writeLock.unlock();
        }
    }
}
