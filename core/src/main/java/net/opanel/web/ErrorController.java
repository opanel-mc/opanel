package net.opanel.web;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import net.opanel.OPanel;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ErrorController extends BaseController {
    public ErrorController(OPanel plugin) {
        super(plugin);
    }

    // 404
    public Handler notFound = ctx -> {
        if(ctx.path().startsWith("/api")) return;

        try {
            // 从类路径加载 404 页面
            InputStream inputStream = getClass().getResourceAsStream("/web/404/index.html");
            if (inputStream != null) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                ctx.contentType("text/html");
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result(content);
            } else {
                // 备用方案：尝试加载根目录的 404.html
                InputStream fallbackStream = getClass().getResourceAsStream("/web/404.html");
                if (fallbackStream != null) {
                    String content = new String(fallbackStream.readAllBytes(), StandardCharsets.UTF_8);
                    ctx.contentType("text/html");
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.result(content);
                } else {
                    // 最终备用方案
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.result("Page not found");
                }
            }
        } catch (Exception e) {
            plugin.logger.error("Error serving 404 page: " + e.getMessage());
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("Page not found");
        }
    };
}
