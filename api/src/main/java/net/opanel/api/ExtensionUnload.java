package net.opanel.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the optional unload callback on an {@link Extension} entry class.
 *
 * <p>At most one method may carry this annotation, and it must have the exact
 * signature {@code public void unload()}. OPanel invokes it before invalidating
 * the extension's {@link OPanelAPI} handles and closing the extension class
 * loader. The callback should release extension-owned threads and resources.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtensionUnload {
}
