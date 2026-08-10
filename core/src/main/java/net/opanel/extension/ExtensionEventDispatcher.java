package net.opanel.extension;

import cn.opanel.api.EventHandler;
import cn.opanel.api.event.*;
import net.opanel.OPanel;
import net.opanel.event.*;
import net.opanel.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class ExtensionEventDispatcher {
    private static final Map<Class<?>, EventType> SUPPORTED_EVENTS;

    static {
        SUPPORTED_EVENTS = Map.of(
            PlayerJoinEvent.class, EventType.PLAYER_JOIN,
            PlayerLeaveEvent.class, EventType.PLAYER_LEAVE,
            PlayerMoveEvent.class, EventType.PLAYER_MOVE,
            PlayerGameModeChangeEvent.class, EventType.PLAYER_GAMEMODE_CHANGE,
            PlayerInventoryChangeEvent.class, EventType.PLAYER_INVENTORY_CHANGE
        );
    }

    private final OPanel plugin;
    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final Consumer<OPanelPlayerJoinEvent> joinListener;
    private final Consumer<OPanelPlayerLeaveEvent> leaveListener;
    private final Consumer<OPanelPlayerMoveEvent> moveListener;
    private final Consumer<OPanelPlayerGameModeChangeEvent> gameModeChangeListener;
    private final Consumer<OPanelPlayerInventoryChangeEvent> inventoryChangeListener;
    private volatile boolean accepting = true;

    public ExtensionEventDispatcher(OPanel plugin) {
        this.plugin = plugin;
        joinListener = event -> dispatch(EventType.PLAYER_JOIN, event);
        leaveListener = event -> dispatch(EventType.PLAYER_LEAVE, event);
        moveListener = event -> dispatch(EventType.PLAYER_MOVE, event);
        gameModeChangeListener = event -> dispatch(EventType.PLAYER_GAMEMODE_CHANGE, event);
        inventoryChangeListener = event -> dispatch(EventType.PLAYER_INVENTORY_CHANGE, event);

        EventManager.get().on(EventType.PLAYER_JOIN, joinListener);
        EventManager.get().on(EventType.PLAYER_LEAVE, leaveListener);
        EventManager.get().on(EventType.PLAYER_MOVE, moveListener);
        EventManager.get().on(EventType.PLAYER_GAMEMODE_CHANGE, gameModeChangeListener);
        EventManager.get().on(EventType.PLAYER_INVENTORY_CHANGE, inventoryChangeListener);
    }

    public void activate(LoadedExtension extension) {
        if(extension.eventHandlers.isEmpty()) return;
        if(!accepting) throw new IllegalStateException("The extension event dispatcher is shutting down.");

        registrations.add(new Registration(extension));
    }

    public void deactivate(LoadedExtension extension) {
        Registration registration = null;
        for(Registration candidate : registrations) {
            if(candidate.extension == extension) {
                registration = candidate;
                break;
            }
        }
        if(registration == null) return;

        registration.stopAccepting();
        registrations.remove(registration);
        registration.awaitIdle();
    }

    public void shutdown() {
        if(!accepting) return;
        accepting = false;

        EventManager.get().off(EventType.PLAYER_JOIN, joinListener);
        EventManager.get().off(EventType.PLAYER_LEAVE, leaveListener);
        EventManager.get().off(EventType.PLAYER_MOVE, moveListener);
        EventManager.get().off(EventType.PLAYER_GAMEMODE_CHANGE, gameModeChangeListener);
        EventManager.get().off(EventType.PLAYER_INVENTORY_CHANGE, inventoryChangeListener);

        List<Registration> snapshot = new ArrayList<>(registrations);
        for(Registration registration : snapshot) registration.stopAccepting();
        registrations.clear();
        for(Registration registration : snapshot) registration.awaitIdle();
    }

    public static Map<EventType, List<Method>> findEventHandlers(Class<?> entryClass) {
        List<Method> annotatedMethods = Utils.findAnnotatedMethods(entryClass, EventHandler.class);
        annotatedMethods.sort(Comparator.comparing(Method::toGenericString));

        Map<EventType, List<Method>> eventHandlers = new EnumMap<>(EventType.class);
        for(Method method : annotatedMethods) {
            validateEventHandler(entryClass, method);
            EventType eventType = SUPPORTED_EVENTS.get(method.getParameterTypes()[0]);
            eventHandlers.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(method);
        }

        Map<EventType, List<Method>> immutableHandlers = new EnumMap<>(EventType.class);
        for(Map.Entry<EventType, List<Method>> entry : eventHandlers.entrySet()) {
            immutableHandlers.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutableHandlers);
    }

    private static void validateEventHandler(Class<?> entryClass, Method method) {
        String description = entryClass.getName() + "#" + method.toGenericString();
        int modifiers = method.getModifiers();

        // The event handler methods must go through these checks,
        // otherwise the extension can't be registered
        if(!Modifier.isPublic(modifiers)) {
            throw invalidHandler(description, "the method must be public");
        }
        if(Modifier.isStatic(modifiers)) {
            throw invalidHandler(description, "the method must not be static");
        }
        if(method.getReturnType() != Void.TYPE) {
            throw invalidHandler(description, "the method must return void");
        }
        if(method.getParameterCount() != 1) {
            throw invalidHandler(description, "the method must accept exactly one event parameter");
        }

        Class<?> eventClass = method.getParameterTypes()[0];
        if(!SUPPORTED_EVENTS.containsKey(eventClass)) {
            throw invalidHandler(description, "the event type is not supported by this OPanel version");
        }
    }

    private static IllegalArgumentException invalidHandler(String method, String reason) {
        return new IllegalArgumentException("Invalid @EventHandler method " + method + ": " + reason + ".");
    }

    private void dispatch(EventType eventType, OPanelEvent event) {
        if(!accepting) return;

        for(Registration registration : registrations) {
            try {
                registration.dispatch(eventType, event);
            } catch (Throwable e) {
                logFailure(
                        "Failed to dispatch " + eventType + " to extension '" + registration.extension.id + "'.",
                        e
                );
            }
        }
    }

    private void logHandlerFailure(LoadedExtension extension, EventType eventType, Method method, Throwable error) {
        logFailure(
                "Extension '" + extension.id + "' failed while handling " + eventType
                        + " in " + method.toGenericString() + ".",
                error
        );
    }

    private void logFailure(String message, Throwable error) {
        try {
            StringWriter stackTrace = new StringWriter();
            error.printStackTrace(new PrintWriter(stackTrace));
            plugin.logger.error(message + System.lineSeparator() + stackTrace);
        } catch (Throwable e) {
            // Extension failures must never escape into the platform event bus
        }
    }

    private final class Registration {
        private final LoadedExtension extension;
        private final ReentrantReadWriteLock dispatchLock = new ReentrantReadWriteLock();
        private volatile boolean active = true;

        private Registration(LoadedExtension extension) {
            this.extension = extension;
        }

        private void dispatch(EventType eventType, OPanelEvent event) {
            List<Method> handlers = extension.eventHandlers.get(eventType);
            if(handlers == null) return;

            dispatchLock.readLock().lock();
            try {
                if(!active) return;

                Object eventObjectForHandler = event.toAPIEvent(extension.api);
                invokeHandlers(eventType, eventObjectForHandler, handlers);
            } finally {
                dispatchLock.readLock().unlock();
            }
        }

        private void invokeHandlers(EventType eventType, Object event, List<Method> handlers) {
            for(Method handler : handlers) {
                invokeHandler(eventType, event, handler);
            }
        }

        private void invokeHandler(EventType eventType, Object event, Method handler) {
            Thread thread = Thread.currentThread();
            ClassLoader previousClassLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(extension.classLoader);
                handler.invoke(extension.instance, event);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                logHandlerFailure(extension, eventType, handler, cause);
            } catch (Throwable e) {
                logHandlerFailure(extension, eventType, handler, e);
            } finally {
                thread.setContextClassLoader(previousClassLoader);
            }
        }

        private void stopAccepting() {
            active = false;
        }

        private void awaitIdle() {
            dispatchLock.writeLock().lock();
            dispatchLock.writeLock().unlock();
        }
    }
}
