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
 *
 * <p>标记 {@link Extension} 入口类上必需的加载回调方法。</p>
 *
 * <p>入口类必须恰好声明一个带此注解的方法，且签名必须为
 * {@code public void load(OPanelAPI api)}。OPanel 会在 Minecraft 服务端实现
 * 可用之后调用它。不得在此回调中直接进行阻塞或修改状态的 API 调用；
 * 扩展应在加载完成后，将此类工作调度到自己的工作线程上执行。</p>
 */
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可通过反射读取
@Target(ElementType.METHOD) // 注解目标为方法
public @interface ExtensionLoad { // 扩展加载回调方法标记注解
}
