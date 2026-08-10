---
name: opanel-extension-dev
description: 指导 OPanel 扩展的创建、实现、审查、调试与兼容性迁移。Use when a task mentions OPanel extension development, extension.json, @Extension lifecycle methods, OPanelAPI, extension backend routes, extension resources/web frontend pages, or OPanel extension event listeners.
---

# OPanel 扩展开发

按照目标 OPanel 版本的公开 API 开发扩展。以用户指定的版本和当前仓库源码为准，不凭记忆补全 API，也不要把 OPanel 本体插件/模组开发方式套用到扩展项目。

## 确认上下文

1. 查找并遵守当前仓库的 `AGENTS.md`、构建约定和验证要求。
2. 检查已有的 `build.gradle`、`extension.json`、入口类和 `src/main/resources/web`，优先延续现有结构。
3. 明确目标 OPanel 版本。按以下优先级确定事实：
   - 用户明确指定的目标版本；
   - 目标仓库中的 `api/` 源码、Javadoc、`example-extension/` 和构建配置；
   - 用户给出的扩展开发指南；
   - 其他资料或模型记忆。
4. 若在 OPanel 主仓库中工作，先检查 `api/src/main/java/cn/opanel/api` 与 `example-extension`。若在独立扩展仓库中工作，核对目标版本实际发布的 `opanel-api` artifact。
5. 遇到指南、示例与源码不一致时，说明差异并采用目标运行版本的 API，不静默猜测。

参考指南的基线是 OPanel `2.2.0-pre5` 及以上、JDK 14 及以上，并示例使用 Javalin `5.6.4`。这些是该指南分支的基线，不代表所有未来版本；实现前仍要核对目标版本。

## 建立项目

独立 Gradle 项目使用 Java 插件，并把 OPanel API 声明为 `compileOnly`：

```groovy
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'cn.opanel:opanel-api:<OPanel version>'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(14)
    }
}
```

- 将 `<OPanel version>` 替换为目标服务端实际版本，不保留占位符。
- 在 OPanel 多模块仓库内开发示例扩展时，优先使用 `compileOnly(project(":api"))`。
- 仅在注册后端路由时添加与目标 OPanel 匹配的 Javalin `compileOnly` 依赖；指南基线为 `io.javalin:javalin:5.6.4`。
- 不要把 `opanel-api` 或 OPanel 提供的 Javalin 打进扩展 JAR。扩展自己的运行时第三方依赖必须随 JAR 提供；需要 Shadow 时，检查最终 JAR 与类加载冲突。

## 编写元数据

创建 `src/main/resources/extension.json`：

```json
{
  "extId": "example-extension",
  "version": "1.0.0",
  "name": "Example Extension",
  "description": "An example OPanel extension.",
  "author": "Author Name"
}
```

保留并填写全部五个字段。`extId` 必须在所有已安装扩展中唯一；兼容当前实现时使用不超过 64 个字符的小写 kebab-case：`[a-z0-9]+(?:-[a-z0-9]+)*`。不要用显示名称、空格或下划线充当 ID。

## 实现入口与生命周期

一个扩展 JAR 必须恰好包含一个 `@Extension` 入口类：

```java
package com.example.extension;

import cn.opanel.api.Extension;
import cn.opanel.api.ExtensionLoad;
import cn.opanel.api.ExtensionUnload;
import cn.opanel.api.OPanelAPI;

@Extension
public final class Main {
    private OPanelAPI api;

    @ExtensionLoad
    public void load(OPanelAPI api) {
        this.api = api;
        api.logInfo("Example extension loaded");
    }

    @ExtensionUnload
    public void unload() {
        api.logInfo("Example extension unloaded");
        // 停止扩展创建的线程、定时器并释放其他资源。
        api = null;
    }
}
```

严格保持以下契约：

- 入口类为 `public`、非抽象类，并提供 `public` 无参构造器。
- `@ExtensionLoad` 恰好一个，签名必须是 `public void load(OPanelAPI api)`。
- `@ExtensionUnload` 最多一个，签名必须是 `public void unload()`。
- 仅在加载完成到卸载开始之间使用传入的 API handle；卸载后调用会失败。
- 不在生命周期回调中执行可能阻塞或改变服务器状态的 API 操作。需要时创建扩展自有 worker，并在 `unload()` 中可靠关闭。
- 只依赖 `cn.opanel.api.*` 公共 API；除非任务明确要求修改 OPanel 本体，否则不要从扩展直接导入 `net.opanel.*` 内部实现。

## 注册后端路由

通过 `OPanelAPI.addHandler` 注册扩展命名空间内的相对路径：

```java
import io.javalin.http.HandlerType;

api.addHandler("status", HandlerType.GET, ctx -> {
    ctx.json(java.util.Map.of("ok", true));
});
```

该路由暴露为 `/api/extension/<extId>/status`。

- 路径开头的 `/` 可省略；不要使用反斜杠或父目录片段。
- 使用 Javalin 5 `Context`/`HandlerType` 写法，并以目标 OPanel 所用版本为准。
- 当前实现按规范化后的路径保存路由；同一路径再次注册会替换旧 handler。若要在同一路径注册多个 HTTP 方法，先检查目标版本是否支持，不能想当然。
- 把 handler 当成服务器请求线程上的代码：校验输入、明确状态码、避免长时间阻塞，并处理 API 异常。
- 扩展后端位于 OPanel 鉴权范围内。用已登录面板会话测试，不要把它误当作无需鉴权的 Open API。

## 添加前端页面

把静态 HTML、CSS 和 JavaScript 放在 `src/main/resources/web`，并提供 `web/index.html`：

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Example Extension</title>
  <link rel="stylesheet" href="assets/style.css">
</head>
<body>
  <p>Hello World</p>
  <script src="assets/main.js"></script>
</body>
</html>
```

- 用户入口使用 `/panel/ext/<extId>/`。
- 当前面板通过 iframe 加载扩展的 `web/index.html`；把页面设计成独立静态应用，不假设能直接使用 OPanel 的 React 组件树或上下文。
- 静态资源使用相对于 `index.html` 的 URL。当前内部资源命名空间为 `/api/extension-res/<extId>/...`，不要把它写死，除非确实需要绝对地址。
- 前端调用扩展后端时使用 `/api/extension/<extId>/...`，保留登录 cookie，并处理 401、404 与服务端错误。
- 若目标版本的文档宣称支持 `/panel/ext/<extId>/<asset-path>`，先与实际前端路由和资源控制器核对；不同版本可能只有扩展首页入口。

## 监听事件

只在 `@Extension` 主类中声明事件 handler：

```java
import cn.opanel.api.EventHandler;
import cn.opanel.api.event.PlayerJoinEvent;

@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    api.logInfo("Player joined: " + event.getPlayer().getName());
}
```

- 方法必须为 `public`、非 `static`、返回 `void`，且恰好接收一个受支持的事件类型。
- 从目标版本的 `cn.opanel.api.event` 包或事件分发器确认支持列表；不要仅凭类名猜测。
- 当前参考实现支持 `PlayerJoinEvent`、`PlayerLeaveEvent`、`PlayerMoveEvent`、`PlayerGameModeChangeEvent` 和 `PlayerInventoryChangeEvent`。
- handler 在产生事件的线程上同步执行，不保证是 Minecraft 主线程。保持快速、非阻塞；将 I/O 或耗时工作转移到扩展自有 worker，并处理生命周期竞态。

## 使用 OPanel API

从 `OPanelAPI` 获取稳定 handle，并针对请求的功能读取对应接口 Javadoc：

- `getServer()`：服务端信息、玩家、游戏规则、白名单、保存与命令。
- `getPluginsAPI()`：插件/模组列表与启停。
- `getLogsAPI()`：日志列表、读取与归档日志删除。
- `getTasksAPI()`：持久化定时任务的创建、更新、启停与删除。
- `getMonitor()`：当前监控快照与历史记录。
- `logInfo`、`logWarn`、`logError`：带扩展名称前缀的日志。
- `addHandler`：扩展后端路由。

遵循以下 API 语义：

- 将返回的集合和值对象视为不可变快照；通过对应 API 方法修改状态。
- `PlayerAPI` 是按 UUID 绑定的稳定 handle，每次调用重新解析最新状态；玩家可能离线或不可用。
- 对可能阻塞的 mutation 使用 worker，不在生命周期回调、事件 handler 或 Minecraft 主线程直接执行。
- 显式处理 `APIUnavailableException`、`ServerUnavailableException`、`PlayerUnavailableException`、`InvalidPlayerStateException`、`OperationFailedException`。把 `ActLaterException` 视为“更改已接受但需稍后/重启生效”，不要误报为完全失败。
- 不把当前 API 没有暴露的能力通过反射或内部类绕过；需要新增公共能力时，单独提出 OPanel API 变更。

## 验证

按改动风险执行以下检查，并遵守仓库对构建命令的限制：

1. 运行允许的 Gradle 编译或构建任务，确认使用目标 Java 与 API 版本。
2. 检查构建 JAR 至少包含根目录 `extension.json`、唯一入口类，以及需要前端时的 `web/index.html` 和静态资源。
3. 确认 JAR 未错误捆绑 OPanel API/Javalin，且扩展自身所需运行时依赖完整。
4. 将 JAR 放入测试服务端的 `opanel/extensions`，启动并检查加载日志；不要直接改动生产服务端。
5. 登录面板后测试 `/api/extension/<extId>/...` 和 `/panel/ext/<extId>/`，覆盖成功、未授权、无效输入与不存在资源。
6. 触发每个事件 handler，确认异常不会破坏事件线程，耗时工作不会阻塞事件分发。
7. 停止 OPanel，确认 `unload()` 释放 worker、定时器、文件和网络资源。
8. 交付时列出修改文件、目标 OPanel 版本、兼容性假设和实际执行的验证；未运行的测试要明确说明。

## 常见故障定位

- `Missing extension.json`：确认文件位于 JAR 根目录，不是 `web/` 或源码包目录。
- `expected exactly one @Extension entry`：删除重复入口，或确认入口类实际打进 JAR。
- `@ExtensionLoad method must be...`：逐字核对公开性、方法名、返回值和参数类型。
- `event type is not supported`：改用目标版本事件包中确实由分发器注册的具体类型。
- 后端 404：核对 `extId`、规范化路径、HTTP 方法、路由是否在 `load()` 中完成注册。
- 前端 404：确认扩展已加载、`web/index.html` 存在、资源路径大小写与 JAR entry 一致。
- `ClassNotFoundException`：区分 API/Javalin（应为 `compileOnly`）和扩展第三方库（应打包进扩展 JAR），再检查 Shadow/relocation。
- 卸载后 API 报不可用：停止后台任务，避免在 `unload()` 返回后继续持有或调用 API handle。

## 官方文档

- [快速入门](https://opanel.cn/docs/extension/quick-start)
- [`extension.json`](https://opanel.cn/docs/extension/extension-json)
- [事件监听](https://opanel.cn/docs/extension/events)
- [前端页面](https://opanel.cn/docs/extension/frontend)
- [后端接口](https://opanel.cn/docs/extension/backend)
