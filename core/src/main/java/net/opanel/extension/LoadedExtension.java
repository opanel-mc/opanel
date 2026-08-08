package net.opanel.extension;

import net.opanel.extension.api.ExtensionAPI;

import java.lang.reflect.Method;
import java.nio.file.Path;
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
}
