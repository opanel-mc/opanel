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
 *
 * <p>标记 OPanel 扩展的入口类。</p>
 *
 * <p>扩展 JAR 中必须恰好包含一个带此注解的类。该类必须是 public、非抽象的，
 * 并且提供一个 public 的无参构造函数。其生命周期方法通过 {@link ExtensionLoad}
 * 声明，并可选地通过 {@link ExtensionUnload} 声明。</p>
 */
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可通过反射读取
@Target(ElementType.TYPE) // 注解目标为类型（类、接口、枚举等）
public @interface Extension { // OPanel 扩展入口类标记注解
}
