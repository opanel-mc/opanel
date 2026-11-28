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
 *
 * <p>标记由 {@link Extension} 入口类声明的事件回调方法。</p>
 *
 * <p>该方法必须是 public、非 static 的，返回 {@code void}，并且恰好接受一个
 * 受支持的、带 {@link ExtensionEvent} 注解的事件类型。处理器在产生事件的线程上
 * 同步执行；必须快速完成，不得阻塞，且不得假设其运行在 Minecraft 主线程上。</p>
 */
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可通过反射读取
@Target(ElementType.METHOD) // 注解目标为方法
public @interface EventHandler { // 事件回调方法标记注解
}
