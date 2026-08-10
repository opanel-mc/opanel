package cn.opanel.api;

import cn.opanel.api.event.ExtensionEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an event callback declared by an {@link Extension} entry class.
 *
 * <p>The method must be public, non-static, return {@code void}, and accept
 * exactly one supported {@link ExtensionEvent}-annotated
 * event type. Handlers run synchronously on the thread that produced the event;
 * they must complete quickly, must not block, and must not assume that they run
 * on the Minecraft main thread.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
}
