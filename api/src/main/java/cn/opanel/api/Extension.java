package cn.opanel.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the entry class of an OPanel extension.
 *
 * <p>An extension JAR must contain exactly one annotated class. The class must
 * be public, non-abstract, and expose a public no-argument constructor. Its
 * lifecycle methods are declared with {@link ExtensionLoad} and optionally
 * {@link ExtensionUnload}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {
}
