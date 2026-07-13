package net.opanel.task;

import net.opanel.common.OPanelServer;
import net.opanel.time.TPS;
import net.opanel.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class TaskBuiltinVariableInjector {
    private final Map<String, String> builtinVariables = new HashMap<>();

    public TaskBuiltinVariableInjector(OPanelServer server) {
        builtinVariables.put("version", server.getVersion());
        builtinVariables.put("tps", String.format("%.2f", TPS.getRecentTPS()));
        builtinVariables.put("motd", server.getMotd().replaceAll("\\n", ""));
        builtinVariables.put("maxPlayerCount", String.valueOf(server.getMaxPlayerCount()));
        builtinVariables.put("ingameTime", Utils.gameTickToTime(server.getIngameTime()));
    }

    public String inject(String rawStr) {
        for(String varName : builtinVariables.keySet()) {
            rawStr = rawStr.replaceAll("@\\{"+ varName +"}", builtinVariables.get(varName));
        }
        return rawStr;
    }
}
