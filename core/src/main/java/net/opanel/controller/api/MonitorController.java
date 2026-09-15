package net.opanel.controller.api;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.monitor.ActivityData;
import net.opanel.monitor.MonitorData;
import net.opanel.monitor.MonitorHistoryManager;
import net.opanel.monitor.MonitorHistoryQueryResult;
import net.opanel.utils.DateAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MonitorController extends BaseController {
    public MonitorController(OPanel plugin) {
        super(plugin);
    }

    public Handler getMonitorSnapshot = ctx -> { // for mcp
        MonitorData data = plugin.getMonitorManager().getSnapshot();
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("cpu", data.cpu());
        obj.put("memory", data.memory());
        obj.put("jvmMemory", data.jvmMemory());
        obj.put("tps", data.tps());
        obj.put("networkUpload", data.networkUpload());
        obj.put("networkDownload", data.networkDownload());
        obj.put("diskRead", data.diskRead());
        obj.put("diskWrite", data.diskWrite());
        sendResponse(ctx, obj);
    };

    public Handler getActivity = ctx -> {
        List<HashMap<String, Object>> activities = new ArrayList<>();

        for(ActivityData activity : plugin.getActivityManager().getActivities()) {
            HashMap<String, Object> activityObj = new HashMap<>();
            activityObj.put("date", activity.date == null ? null : DateAdapter.dateToString(activity.date));
            activityObj.put("players", activity.players);
            activities.add(activityObj);
        }

        HashMap<String, Object> obj = new HashMap<>();
        obj.put("activities", activities);
        sendResponse(ctx, obj);
    };

    public Handler getHistory = ctx -> {
        String fromText = ctx.queryParam("from");
        String toText = ctx.queryParam("to");
        String maxPointsText = ctx.queryParam("maxPoints");
        if(fromText == null || fromText.isBlank() || toText == null || toText.isBlank()) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "from and to are required.");
            return;
        }

        try {
            long from = Long.parseLong(fromText);
            long to = Long.parseLong(toText);
            int maxPoints = maxPointsText == null || maxPointsText.isBlank()
                    ? MonitorHistoryManager.DEFAULT_MAX_POINTS
                    : Integer.parseInt(maxPointsText);
            MonitorHistoryQueryResult result = plugin.getMonitorManager().queryHistory(from, to, maxPoints);

            HashMap<String, Object> obj = new HashMap<>();
            obj.put("from", result.from());
            obj.put("to", result.to());
            obj.put("resolutionMs", result.resolutionMs());
            obj.put("points", result.points());
            sendResponse(ctx, obj);
        } catch (IllegalArgumentException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            sendResponse(ctx, HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    };
}
