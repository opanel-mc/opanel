package net.opanel.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import net.opanel.OPanel;
import net.opanel.common.OPanelServer;
import net.opanel.web.BaseController;

import java.util.HashMap;

/**
 * 版本信息控制器 - 处理服务器版本相关的API请求
 * 提供获取服务器类型和版本信息的接口
 */
public class VersionController extends BaseController {
    
    /**
     * 构造函数
     * @param plugin OPanel插件实例
     */
    public VersionController(OPanel plugin) {
        super(plugin);
    }

    public Handler getVersion = ctx -> {
        
        final OPanelServer server = plugin.getServer();
        
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("serverType", server.getServerType().getName());
        obj.put("version", server.getVersion());
        
        sendResponse(ctx, obj);
    };
}