package net.opanel.web;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.opanel.OPanel;
import net.opanel.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.HashMap;

public abstract class BaseServlet extends HttpServlet {
    protected final OPanel plugin;

    public BaseServlet(OPanel plugin) {
        this.plugin = plugin;
    }

    protected void sendResponse(HttpServletResponse res, String msg) {
        sendResponse(res, HttpServletResponse.SC_OK, msg);
    }

    protected void sendResponse(HttpServletResponse res, HashMap<String, Object> jsonObj) {
        jsonObj.put("code", 200);
        jsonObj.put("error", "");
        sendResponse(res, new Gson().toJson(jsonObj));
    }

    protected void sendResponse(HttpServletResponse res, int code) {
        HashMap<String, Object> obj = new HashMap<>();
        obj.put("code", code);
        switch(code) {
            case HttpServletResponse.SC_BAD_REQUEST -> obj.put("error", "Bad request");
            case HttpServletResponse.SC_UNAUTHORIZED -> obj.put("error", "Unauthorized");
            case HttpServletResponse.SC_FORBIDDEN -> obj.put("error", "Forbidden");
            case HttpServletResponse.SC_NOT_FOUND -> obj.put("error", "Not found");
            case HttpServletResponse.SC_CONFLICT -> obj.put("error", "Conflict");
            case HttpServletResponse.SC_INTERNAL_SERVER_ERROR -> obj.put("error", "Server internal error");
        }
        sendResponse(res, code, new Gson().toJson(obj));
    }

    protected void sendResponse(HttpServletResponse res, int code, String msg) {
        res.addHeader("X-Powered-By", "OPanel");
        res.setStatus(code);
        res.setContentType("application/json");
        res.setCharacterEncoding("utf-8");

        try(OutputStream os = res.getOutputStream()) {
            os.write(msg.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendContentResponse(HttpServletResponse res, byte[] bytes, String contentType) {
        res.addHeader("X-Powered-By", "OPanel");
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType(contentType);
        res.setCharacterEncoding("utf-8");

        try(OutputStream os = res.getOutputStream()) {
            os.write(bytes);
        } catch (IOException e) {
            sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    protected <T> T getRequestBody(HttpServletRequest req, Class<T> type) throws IOException {
        try (BufferedReader reader = req.getReader()) {
            String line;
            StringBuilder requestBody = new StringBuilder();
            while((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            Gson gson = new Gson();
            return gson.fromJson(requestBody.toString(), type);
        }
    }

    protected <T> T getRequestBody(HttpServletRequest req, Type type) throws IOException {
        try (BufferedReader reader = req.getReader()) {
            String line;
            StringBuilder requestBody = new StringBuilder();
            while((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            Gson gson = new Gson();
            return gson.fromJson(requestBody.toString(), type);
        }
    }

    protected boolean authCookie(HttpServletRequest req) {
        String token = req.getHeader("X-Credential-Token"); // salted hashed 3
        if(token == null) return false;

        final String hashedRealKey = plugin.getConfig().accessKey; // hashed 2
        return token.equals(Utils.md5(plugin.getConfig().salt + hashedRealKey));
    }
}
