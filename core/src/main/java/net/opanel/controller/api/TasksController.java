package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.exception.IllegalTaskCommandSyntaxException;
import net.opanel.task.ScheduledTask;
import net.opanel.task.ScheduledTaskManager;
import net.opanel.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class TasksController extends BaseController {
    private ScheduledTaskManager scheduledTaskManager;

    public TasksController(OPanel plugin) {
        super(plugin);

        scheduledTaskManager = plugin.getScheduledTaskManager();
    }

    public Handler getTasks = ctx -> {
        HashMap<String, Object> obj = new HashMap<>();

        List<HashMap<String, Object>> serializedTaskList = new ArrayList<>();
        for(ScheduledTask task : scheduledTaskManager.getTasks()) {
            HashMap<String, Object> serializedTask = new HashMap<>();
            serializedTask.put("id", task.getId());
            serializedTask.put("name", Utils.stringToBase64(task.getName()));
            serializedTask.put("cron", task.getCron());
            serializedTask.put("commands", task.getCommands());
            serializedTask.put("enabled", task.isEnabled());
            serializedTaskList.add(serializedTask);
        }

        obj.put("tasks", serializedTaskList);
        sendResponse(ctx, obj);
    };

    public Handler createTask = ctx -> {
        try {
            TaskEditRequestBodyType reqBody = ctx.bodyAsClass(TaskEditRequestBodyType.class);
            ScheduledTask task = scheduledTaskManager.createTask(Utils.base64ToString(reqBody.name), reqBody.cron, reqBody.commands);

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("taskId", task.getId());
            sendResponse(ctx, obj);
        } catch (IllegalTaskCommandSyntaxException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal commands syntax: "+ e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal cron expression: "+ e.getMessage());
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler editTask = ctx -> {
        final String id = ctx.pathParam("id");
        
        try {
            TaskEditRequestBodyType reqBody = ctx.bodyAsClass(TaskEditRequestBodyType.class);
            scheduledTaskManager.setTaskCron(id, reqBody.cron);
            scheduledTaskManager.setTaskName(id, reqBody.name);
            scheduledTaskManager.setTaskCommands(id, reqBody.commands);
            sendResponse(ctx, HttpStatus.OK);
        } catch (IllegalTaskCommandSyntaxException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal commands syntax: "+ e.getMessage());
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Illegal cron expression: "+ e.getMessage());
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Task not found: " + id);
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler toggleTask = ctx -> {
        final String id = ctx.pathParam("id");
        final String enabled = ctx.queryParam("enabled");
        if(enabled == null) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Enabled status is missing.");
            return;
        }

        try {
            scheduledTaskManager.setTaskEnabled(id, enabled.equals("1"));
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Task not found: " + id);
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    public Handler deleteTask = ctx -> {
        final String id = ctx.pathParam("id");
        
        try {
            scheduledTaskManager.deleteTask(id);
            sendResponse(ctx, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            sendResponse(ctx, HttpStatus.NOT_FOUND, "Task not found: " + id);
        } catch (Exception e) {
            sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    };

    private static class TaskEditRequestBodyType {
        String name;
        String cron;
        List<String> commands;
    }
}
