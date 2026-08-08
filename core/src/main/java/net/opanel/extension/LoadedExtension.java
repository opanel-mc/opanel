package net.opanel.extension;

import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import net.opanel.extension.api.ExtensionAPI;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LoadedExtension {
    public final String id;
    public final ExtensionMetadata metadata;
    public final Path sourceJar;
    public final Object instance;
    public final Method loadMethod;
    public final Method unloadMethod;
    public final ExtensionAPI api;
    public final ExtensionManager.ExtensionClassLoader classLoader;
    public final JarFile jarFile;
    public final Map<String, BackendRoute> backendRoutesMap = new LinkedHashMap<>();

    public LoadedExtension(
            String id,
            ExtensionMetadata metadata,
            Path sourceJar,
            Object instance,
            Method loadMethod,
            Method unloadMethod,
            ExtensionAPI api,
            ExtensionManager.ExtensionClassLoader classLoader,
            JarFile jarFile
    ) {
        this.id = id;
        this.metadata = metadata;
        this.sourceJar = sourceJar;
        this.instance = instance;
        this.loadMethod = loadMethod;
        this.unloadMethod = unloadMethod;
        this.api = api;
        this.classLoader = classLoader;
        this.jarFile = jarFile;
    }

    public boolean hasResource(String resourcePath) {
        JarEntry entry = jarFile.getJarEntry(resourcePath);
        return entry != null && !entry.isDirectory();
    }

    public void addHandler(String path, HandlerType method, Handler handler) {
        BackendRoute route = new BackendRoute();
        route.method = method;
        route.handler = handler;
        backendRoutesMap.put(path, route);
    }

    public BackendRoute getBackendRoute(String path) {
        return backendRoutesMap.get(path);
    }

    public static class BackendRoute {
        public HandlerType method;
        public Handler handler;
    }
}
