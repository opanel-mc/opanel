package net.opanel.controller.api;

import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.monitor.ActivityData;
import net.opanel.monitor.MonitorData;
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
}
