package net.opanel.monitor;
import net.opanel.OPanel;
import net.opanel.config.*;
import net.opanel.controller.BeforeController;
import net.opanel.controller.api.MonitorController;
import net.opanel.controller.api.MapController;
import net.opanel.controller.api.DiagnosticsController;
import net.opanel.logger.Loggable;
import net.opanel.web.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.List;
import static io.javalin.apibuilder.ApiBuilder.*;

/** Starts the actual controller and authorization middleware on loopback. Run in an isolated directory. */
public class PerformanceIncidentHttpTest {
    @org.junit.Test
    public void verifiesBehavior() throws Exception { main(new String[0]); }

    public static void main(String[] args) throws Exception {
        OPanelConfiguration config = OPanelConfiguration.defaultConfig;
        config.accessKey = "test-only-access";
        config.salt = "test-only-salt";
        config.monitorHistoryEnabled = false;
        config.diagnosticsEnabled = true;
        Loggable log = new Loggable() {
            public void info(String value) { System.out.println(value); }
            public void warn(String value) { System.out.println(value); }
            public void error(String value) { System.err.println(value); }
        };
        OPanel panel = new OPanel(new ConfigManager() {
            public OPanelConfiguration get() { return config; }
            public void set(OPanelConfiguration value) { }
        }, log);
        BeforeController auth = new BeforeController(panel);
        MonitorController monitor = new MonitorController(panel);
        MapController map = new MapController(panel);
        DiagnosticsController diagnostics = new DiagnosticsController(panel);
        Javalin app = Javalin.create(c -> {
            c.jsonMapper(new JavalinGson(new Gson(), false));
            c.routes.apiBuilder(() -> {
                beforeMatched("/*", auth.authToken);
                get("/api/monitor/incidents", monitor.getIncidents, AuthRouteRole.PANEL_OR_MCP);
                get("/api/map-info/player-positions", map.getPlayerPositions, AuthRouteRole.PANEL_OR_MCP);
                get("/api/diagnostics/settings", diagnostics.getSettings, AuthRouteRole.PANEL_SESSION);
                post("/api/diagnostics/settings", diagnostics.saveSettings, AuthRouteRole.PANEL_SESSION);
            });
        }).start("127.0.0.1", 0);
        try {
            String base = "http://127.0.0.1:" + app.port() + "/api/monitor/incidents";
            HttpClient client = HttpClient.newHttpClient();
            if(client.send(HttpRequest.newBuilder(URI.create(base)).build(), HttpResponse.BodyHandlers.ofString()).statusCode() != 401) throw new AssertionError("anonymous access allowed");
            PerformanceIncidentRecorder recorder = new PerformanceIncidentRecorder(50, 2, 2);
            long now = System.currentTimeMillis();
            recorder.record(now - 1000, 80);
            recorder.record(now, 90);
            PerformanceTickSampler.restore(recorder.snapshot());
            String token = JwtManager.generateToken(config.accessKey, config.salt);
            var response = client.send(HttpRequest.newBuilder(URI.create(base)).header("Cookie", "token=" + token).build(), HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) throw new AssertionError(response.body());
            var rows = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("incidents");
            if(rows.size() != 1 || !rows.get(0).getAsJsonObject().getAsJsonArray("samples").isEmpty()) throw new AssertionError("list includes bulky evidence");
            String id = rows.get(0).getAsJsonObject().get("id").getAsString();
            response = client.send(HttpRequest.newBuilder(URI.create(base + "?id=" + id)).header("Cookie", "token=" + token).build(), HttpResponse.BodyHandlers.ofString());
            if(JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("incidents").get(0).getAsJsonObject().getAsJsonArray("samples").size() != 2) throw new AssertionError("detail lost evidence");
            String positionsUrl = "http://127.0.0.1:" + app.port() + "/api/map-info/player-positions";
            if(client.send(HttpRequest.newBuilder(URI.create(positionsUrl)).build(), HttpResponse.BodyHandlers.ofString()).statusCode() != 401) throw new AssertionError("anonymous positions access allowed");
            net.opanel.map.MapPlayerPositions.publish(System.currentTimeMillis(), List.of(
                    new net.opanel.map.MapPlayerPositions.Position("online-id", "Alex", "world_nether", -10, 64, 20)));
            response = client.send(HttpRequest.newBuilder(URI.create(positionsUrl)).header("Cookie", "token=" + token).build(), HttpResponse.BodyHandlers.ofString());
            var positions = JsonParser.parseString(response.body()).getAsJsonObject();
            if(response.statusCode() != 200 || !positions.get("available").getAsBoolean()
                    || !positions.getAsJsonArray("players").get(0).getAsJsonObject().get("world").getAsString().equals("world_nether")) throw new AssertionError("world-aware positions missing");
            net.opanel.map.MapPlayerPositions.publish(1, List.of(
                    new net.opanel.map.MapPlayerPositions.Position("stale-id", "Alex", "world", 0, 64, 0)));
            response = client.send(HttpRequest.newBuilder(URI.create(positionsUrl)).header("Cookie", "token=" + token).build(), HttpResponse.BodyHandlers.ofString());
            if(!JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("players").isEmpty()) throw new AssertionError("stale positions leaked");
            net.opanel.monitor.DiagnosticsState.configure(false);
            net.opanel.map.MapPlayerPositions.reset();
            response = client.send(HttpRequest.newBuilder(URI.create(positionsUrl)).header("Cookie", "token=" + token).build(), HttpResponse.BodyHandlers.ofString());
            if(JsonParser.parseString(response.body()).getAsJsonObject().get("enabled").getAsBoolean()
                    || net.opanel.map.MapPlayerPositions.hasDemand()) throw new AssertionError("disabled diagnostics started sampling");
            String settingsUrl = "http://127.0.0.1:" + app.port() + "/api/diagnostics/settings";
            config.diagnosticsEnabled = false;
            var enabledBody = HttpRequest.BodyPublishers.ofString("{\"diagnosticsEnabled\":true,\"performanceIncidentsEnabled\":false}");
            response = client.send(HttpRequest.newBuilder(URI.create(settingsUrl)).POST(enabledBody).build(), HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 401) throw new AssertionError("anonymous settings changes allowed");
            response = client.send(HttpRequest.newBuilder(URI.create(settingsUrl)).header("Cookie", "token=" + token).header("Content-Type", "application/json").POST(enabledBody).build(), HttpResponse.BodyHandlers.ofString());
            var saved = JsonParser.parseString(response.body()).getAsJsonObject();
            if(response.statusCode() != 200 || !config.diagnosticsEnabled || !saved.get("restartRequired").getAsBoolean() || DiagnosticsState.enabled()) throw new AssertionError("settings not persisted or restart state incorrect");
            response = client.send(HttpRequest.newBuilder(URI.create(settingsUrl)).header("Cookie", "token=" + token).POST(HttpRequest.BodyPublishers.ofString("{\"diagnosticsEnabled\":\"true\",\"performanceIncidentsEnabled\":false}")).build(), HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 400) throw new AssertionError("invalid settings accepted");
            Files.writeString(Path.of("http-result.txt"), "anonymous=401\nauthenticated=200\nlistEvidence=0\ndetailEvidence=2\n");
            System.out.println("PerformanceIncidentHttpTest passed");
        } finally { app.stop(); panel.stop(); }
    }
}
