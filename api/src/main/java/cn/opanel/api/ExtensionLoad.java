package cn.opanel.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the required load callback on an {@link Extension} entry class.
 *
 * <p>The entry class must declare exactly one annotated method with the exact
 * signature {@code public void load(OPanelAPI api)}. OPanel invokes it after the
 * Minecraft server implementation is available. Blocking or mutating API calls
 * must not be made directly from this callback; extensions should schedule that
 * work on their own worker thread after loading completes.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtensionLoad {
}
