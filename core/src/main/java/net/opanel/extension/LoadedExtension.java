package net.opanel.extension;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LoadedExtension {
    public final String id;
    public final Path sourceJar;
    public final Object instance;
    public final Method loadMethod;
    public final Method unloadMethod;
    public final ExtensionManager.ExtensionClassLoader classLoader;
    public final JarFile jarFile;

    public LoadedExtension(
            String id,
            Path sourceJar,
            Object instance,
            Method loadMethod,
            Method unloadMethod,
            ExtensionManager.ExtensionClassLoader classLoader,
            JarFile jarFile
    ) {
        this.id = id;
        this.sourceJar = sourceJar;
        this.instance = instance;
        this.loadMethod = loadMethod;
        this.unloadMethod = unloadMethod;
        this.classLoader = classLoader;
        this.jarFile = jarFile;
    }

    public boolean hasResource(String resourcePath) {
        JarEntry entry = jarFile.getJarEntry(resourcePath);
        return entry != null && !entry.isDirectory();
    }
}
