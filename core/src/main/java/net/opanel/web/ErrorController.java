package net.opanel.web;

import io.javalin.http.ContentType;
import io.javalin.http.Handler;
import net.opanel.OPanel;

import java.io.InputStream;

public class ErrorController extends BaseController {
    public ErrorController(OPanel plugin) {
        super(plugin);
    }

    // 404
    public Handler notFound = ctx -> {
        if(ctx.path().startsWith("/api") || ctx.path().startsWith("/file")) return;

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("web/404.html")) {
            if(is == null) return;
            sendContent(ctx, is.readAllBytes(), ContentType.TEXT_HTML);
        }
    };
}
