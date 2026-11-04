package net.opanel.controller.api;

import com.google.gson.JsonParser;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;
import net.opanel.controller.BaseController;
import net.opanel.monitor.DiagnosticsState;
import net.opanel.monitor.PerformanceTickSampler;
import java.util.HashMap;

/** Panel-session-only settings; changes are persisted and take effect on restart. */
public class DiagnosticsController extends BaseController {
    public DiagnosticsController(OPanel plugin) { super(plugin); }

    private HashMap<String, Object> settings() {
        var config = plugin.getConfig();
        HashMap<String, Object> result = new HashMap<>();
        result.put("diagnosticsEnabled", config.diagnosticsEnabled);
        result.put("performanceIncidentsEnabled", config.performanceIncidentsEnabled);
        result.put("active", DiagnosticsState.enabled());
        result.put("restartRequired", config.diagnosticsEnabled != DiagnosticsState.enabled()
                || (config.diagnosticsEnabled && config.performanceIncidentsEnabled) != PerformanceTickSampler.enabled());
        return result;
    }

    public Handler getSettings = ctx -> {
        ctx.header("Cache-Control", "no-store");
        sendResponse(ctx, settings());
    };

    public Handler saveSettings = ctx -> {
        final boolean enabled;
        final boolean incidents;
        try {
            var body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            var first = body.getAsJsonPrimitive("diagnosticsEnabled");
            var second = body.getAsJsonPrimitive("performanceIncidentsEnabled");
            if(first == null || second == null || !first.isBoolean() || !second.isBoolean()) throw new IllegalArgumentException();
            enabled = first.getAsBoolean();
            incidents = second.getAsBoolean();
        } catch(RuntimeException e) {
            sendResponse(ctx, HttpStatus.BAD_REQUEST, "Both settings must be booleans.");
            return;
        }
        var config = plugin.getConfig();
        boolean previousEnabled = config.diagnosticsEnabled;
        boolean previousIncidents = config.performanceIncidentsEnabled;
        try {
            config.diagnosticsEnabled = enabled;
            config.performanceIncidentsEnabled = incidents;
            plugin.setConfig(config);
        } catch(RuntimeException e) {
            config.diagnosticsEnabled = previousEnabled;
            config.performanceIncidentsEnabled = previousIncidents;
            throw e;
        }
        sendResponse(ctx, settings());
    };
}
