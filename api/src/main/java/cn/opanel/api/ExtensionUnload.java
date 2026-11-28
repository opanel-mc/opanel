package cn.opanel.api;

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
 *
 * <p>标记 {@link Extension} 入口类上可选的卸载回调方法。</p>
 *
 * <p>至多一个方法可以带此注解，且其签名必须为 {@code public void unload()}。
 * OPanel 会在使扩展的 {@link OPanelAPI} 句柄失效并关闭扩展类加载器之前调用它。
 * 该回调应释放扩展自身拥有的线程和资源。</p>
 */
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可通过反射读取
@Target(ElementType.METHOD) // 注解目标为方法
public @interface ExtensionUnload { // 扩展卸载回调方法标记注解
}
