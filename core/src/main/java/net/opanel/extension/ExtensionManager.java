package net.opanel.extension;

import cn.opanel.api.Extension;
import cn.opanel.api.ExtensionLoad;
import cn.opanel.api.ExtensionUnload;
import cn.opanel.api.OPanelAPI;
import com.google.gson.Gson;
import net.opanel.OPanel;
import net.opanel.event.EventType;
import net.opanel.extension.api.ExtensionAPI;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ExtensionManager {
    private static final String METADATA_FILE = "extension.json";
    private static final String WEB_ROOT = "web/";
    private static final String WEB_INDEX = WEB_ROOT + "index.html";
    private static final String EXTENSION_ID_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    private static final Gson gson = new Gson();

    private final OPanel plugin;
    private final ExtensionEventDispatcher eventDispatcher;
    private final Map<String, LoadedExtension> loadedExtensions = new LinkedHashMap<>();
    private final Set<String> unloadingExtensionIds = new HashSet<>();
    private boolean scanned;
    private boolean unloading;

    public ExtensionManager(OPanel plugin) {
        this.plugin = plugin;
        eventDispatcher = new ExtensionEventDispatcher(plugin);
    }

    public synchronized void loadExtensions() {
        if(scanned || unloading) return;
        scanned = true;

        List<Path> extensionJars = new ArrayList<>();
        try(Stream<Path> paths = Files.list(OPanel.EXTENSIONS_DIR_PATH)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .forEach(extensionJars::add);
        } catch (IOException e) {
            plugin.logger.error("Failed to scan extensions: " + e.getMessage());
            return;
        }

        for(Path extensionJar : extensionJars) {
            loadExtension(extensionJar);
        }
    }

    public void unloadExtensions() {
        List<LoadedExtension> extensions;
        synchronized(this) {
            if(unloading) return;
            unloading = true;
            extensions = new ArrayList<>(loadedExtensions.values());
        }

        try {
            eventDispatcher.shutdown();
            for(int i = extensions.size() - 1; i >= 0; i--) {
                unloadExtension(extensions.get(i));
            }
        } finally {
            synchronized(this) {
                loadedExtensions.clear();
            }
        }
    }

    public synchronized LoadedExtension getExtension(String extensionId) {
        return loadedExtensions.get(extensionId);
    }

    public synchronized boolean hasExtension(String extensionId) {
        return loadedExtensions.containsKey(extensionId);
    }

    public synchronized boolean hasWebIndex(String extensionId) {
        LoadedExtension extension = getExtension(extensionId);
        return extension != null && extension.hasResource(WEB_INDEX);
    }

    public synchronized InputStream openWebResource(String extensionId, String resourcePath) throws IOException {
        LoadedExtension extension = getExtension(extensionId);
        if(extension == null) return null;

        JarEntry entry = extension.jarFile.getJarEntry(WEB_ROOT + resourcePath);
        if(entry == null || entry.isDirectory()) return null;
        return extension.jarFile.getInputStream(entry);
    }

    public synchronized void loadExtension(Path extensionPath) {
        if(unloading) return;

        JarFile jarFile = null;
        ExtensionClassLoader classLoader = null;
        LoadedExtension loadedExtension = null;
        String extensionId = null;
        boolean loadCallbackStarted = false;
        try {
            jarFile = new JarFile(extensionPath.toFile());
            ExtensionMetadata metadata = readMetadata(jarFile);
            extensionId = metadata.extId;
            if(!extensionId.matches(EXTENSION_ID_PATTERN) || extensionId.length() > 64) {
                plugin.logger.error("Skipping extension '" + extensionPath.getFileName() + "': invalid extension id '" + extensionId + "'.");
                return;
            }
            if(loadedExtensions.containsKey(extensionId)) {
                plugin.logger.error("Skipping extension '" + extensionPath.getFileName() + "': extension id '" + extensionId + "' is already loaded.");
                return;
            }
            if(unloadingExtensionIds.contains(extensionId)) {
                plugin.logger.error("Skipping extension '" + extensionPath.getFileName() + "': extension id '" + extensionId + "' is being unloaded.");
                return;
            }

            classLoader = new ExtensionClassLoader(extensionPath.toUri().toURL(), OPanelAPI.class.getClassLoader(), OPanel.class.getClassLoader());
            Class<?> entryClass = findExtensionEntry(jarFile, classLoader, extensionPath);
            if(entryClass == null) return;

            plugin.logger.info("Loading extension '"+ metadata.name +"' v"+ metadata.version);
            validateEntryClass(entryClass);
            Method loadMethod = findLoadMethod(entryClass);
            Method unloadMethod = findUnloadMethod(entryClass);
            Map<EventType, List<Method>> eventHandlers = ExtensionEventDispatcher.findEventHandlers(entryClass);
            Constructor<?> constructor = entryClass.getConstructor();
            Object instance = constructor.newInstance();

            ExtensionAPI api = ExtensionContext.buildApi(plugin, metadata);
            loadedExtension = new LoadedExtension(
                    extensionId,
                    metadata,
                    extensionPath,
                    instance,
                    loadMethod,
                    unloadMethod,
                    api,
                    classLoader,
                    jarFile,
                    eventHandlers
            );

            loadCallbackStarted = true;
            loadedExtensions.put(extensionId, loadedExtension);
            invokeLifecycle(classLoader, loadMethod, instance, api);
            eventDispatcher.activate(loadedExtension);
            jarFile = null;
            classLoader = null;
        } catch (Throwable e) {
            plugin.logger.error("Failed to load extension '" + extensionPath.getFileName() + "': " + describe(e));
            if(loadCallbackStarted) {
                unloadExtension(loadedExtension);
                classLoader = null;
                jarFile = null;
            }
        } finally {
            if(classLoader != null) close(classLoader, extensionPath);
            if(jarFile != null) close(jarFile, extensionPath);
        }
    }

    private Class<?> findExtensionEntry(JarFile jarFile, ClassLoader classLoader, Path extensionPath) {
        List<Class<?>> entries = new ArrayList<>();
        jarFile.stream()
                .filter(entry -> isClassEntry(entry.getName()))
                .forEach(entry -> {
                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    try {
                        Class<?> type = Class.forName(className, false, classLoader);
                        if(type.isAnnotationPresent(Extension.class)) entries.add(type);
                    } catch (Throwable e) {
                        plugin.logger.warn("Unable to inspect class '" + className + "' in extension '" + extensionPath.getFileName() + "': " + describe(e));
                    }
                });

        if(entries.size() != 1) {
            plugin.logger.error("Skipping extension '" + extensionPath.getFileName() + "': expected exactly one @Extension entry, found " + entries.size() + ".");
            return null;
        }
        return entries.get(0);
    }

    public static ExtensionMetadata readMetadata(Path extensionPath) throws IOException {
        try(JarFile jarFile = new JarFile(extensionPath.toFile())) {
            return readMetadata(jarFile);
        }
    }

    private static ExtensionMetadata readMetadata(JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry(METADATA_FILE);
        if(entry == null || entry.isDirectory()) {
            throw new IllegalArgumentException("Missing " + METADATA_FILE + ".");
        }

        try(
                InputStream inputStream = jarFile.getInputStream(entry);
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        ) {
            ExtensionMetadata metadata = gson.fromJson(reader, ExtensionMetadata.class);
            if(metadata == null || metadata.extId == null || metadata.extId.isBlank()) {
                throw new IllegalArgumentException(METADATA_FILE + " must define a non-blank extId.");
            }
            if(metadata.name == null || metadata.name.isBlank()) {
                throw new IllegalArgumentException(METADATA_FILE + " must define a non-blank name.");
            }
            return metadata;
        }
    }

    private static boolean isClassEntry(String name) {
        return (
            name.endsWith(".class")
            && !name.startsWith("META-INF/")
            && !name.startsWith("versions/")
            && !name.endsWith("module-info.class")
        );
    }

    private static void validateEntryClass(Class<?> entryClass) {
        int modifiers = entryClass.getModifiers();
        if(!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers)) {
            throw new IllegalArgumentException("Extension entry must be a public, non-abstract class.");
        }
    }

    private static Method findLoadMethod(Class<?> entryClass) {
        List<Method> methods = Utils.findAnnotatedMethods(entryClass, ExtensionLoad.class);
        if(methods.size() != 1) {
            throw new IllegalArgumentException("Extension entry must declare exactly one @ExtensionLoad method.");
        }

        Method method = methods.get(0);
        if(!Modifier.isPublic(method.getModifiers()) || !method.getName().equals("load")
                || method.getReturnType() != Void.TYPE || method.getParameterCount() != 1
                || method.getParameterTypes()[0] != OPanelAPI.class) {
            throw new IllegalArgumentException("@ExtensionLoad method must be public void load(OPanelAPI api).");
        }
        return method;
    }

    private static Method findUnloadMethod(Class<?> entryClass) {
        List<Method> methods = Utils.findAnnotatedMethods(entryClass, ExtensionUnload.class);
        if(methods.size() > 1) {
            throw new IllegalArgumentException("Extension entry may declare at most one @ExtensionUnload method.");
        }
        if(methods.isEmpty()) return null;

        Method method = methods.get(0);
        if(
                !Modifier.isPublic(method.getModifiers())
                || !method.getName().equals("unload")
                || method.getReturnType() != Void.TYPE
                || method.getParameterCount() != 0
        ) {
            throw new IllegalArgumentException("@ExtensionUnload method must be public void unload().");
        }
        return method;
    }

    private static void invokeLifecycle(ClassLoader classLoader, Method method, Object instance, Object... arguments) throws Throwable {
        if(method == null) return;

        Thread thread = Thread.currentThread();
        ClassLoader previousClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            method.invoke(instance, arguments);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } finally {
            thread.setContextClassLoader(previousClassLoader);
        }
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    public void unloadExtension(LoadedExtension extension) {
        synchronized(this) {
            if(loadedExtensions.get(extension.id) != extension || !unloadingExtensionIds.add(extension.id)) return;
        }

        try {
            eventDispatcher.deactivate(extension);
            try {
                invokeLifecycle(extension.classLoader, extension.unloadMethod, extension.instance);
            } catch (Throwable e) {
                plugin.logger.error("Failed to unload extension '" + extension.id + "': " + describe(e));
            }
        } finally {
            extension.api.invalidate();
            synchronized(this) {
                loadedExtensions.remove(extension.id, extension);
            }
            close(extension.classLoader, extension.sourceJar);
            close(extension.jarFile, extension.sourceJar);
            synchronized(this) {
                unloadingExtensionIds.remove(extension.id);
            }
        }
    }

    private void close(AutoCloseable closeable, Path extensionPath) {
        try {
            closeable.close();
        } catch (Exception e) {
            plugin.logger.error("Failed to close extension '" + extensionPath.getFileName() + "': " + e.getMessage());
        }
    }

    public static class ExtensionClassLoader extends URLClassLoader {
        private final ClassLoader apiClassLoader;
        private final ClassLoader coreClassLoader;

        private ExtensionClassLoader(URL extensionUrl, ClassLoader apiClassLoader, ClassLoader coreClassLoader) {
            super(new URL[] { extensionUrl }, ClassLoader.getPlatformClassLoader());
            this.apiClassLoader = apiClassLoader;
            this.coreClassLoader = coreClassLoader;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Expose OPanel API classes
            if(name.startsWith("cn.opanel.api.")) return apiClassLoader.loadClass(name);

            // Expose Javalin classes
            if(name.startsWith("io.javalin.") || name.startsWith("jakarta.servlet.")) return coreClassLoader.loadClass(name);

            try { // try to load java-provided class
                return ClassLoader.getPlatformClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                //
            }

            synchronized(getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if(loadedClass == null) loadedClass = findClass(name);
                if(resolve) resolveClass(loadedClass);
                return loadedClass;
            }
        }
    }
}
