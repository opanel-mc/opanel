# 插件/模组自动更新 V1 提案

## 状态

仅供评审的草案。

本文档所描述的功能目前尚未实现。本文档用于记录将 OPanel 现有插件/模组更新流程扩展为多来源自动更新系统的 V1 版本建议范围。

## 目标

1. 检测已安装的插件/模组是否存在更新且兼容的版本。
2. 支持多个上游平台，而不是仅支持 Modrinth。
3. 复用 OPanel 现有的安全替换及“延迟到重启时应用”机制。
4. 支持后台自动检查更新，并可选择自动应用更新。
5. 将 V1 的范围控制在能够安全实现和验证的规模内。

## 非目标

1. V1 不提供浏览器内的插件市场搜索界面。
2. 不为所有平台提供尽力而为的模糊匹配。
3. V1 不支持所有插件分发网站。
4. 不保证已加载的插件/模组可以热重载。
5. 未通过文件完整性验证时，不允许进行破坏性覆盖。

## 仓库当前状态

### 已有功能

1. `PluginUpdateManager` 已支持：

   * 基于哈希值在 Modrinth 上识别文件
   * 根据服务器类型和 Minecraft 版本进行兼容性筛选
   * 缓存更新检查结果
   * 下载到临时文件
   * 摘要校验
   * 通过 `server.updatePlugin(...)` 安全替换文件
2. `PluginsController` 已提供：

   * `POST /api/plugins/check-updates`
   * `POST /api/plugins/update`
3. 各平台服务器实现已经支持在文件被锁定或正在使用时延迟应用更新：

   * `.update` 暂存文件
   * `ActLaterException`
   * 安排在服务器重启时移动文件
4. 前端已经提供：

   * 插件更新对话框
   * 进入插件页面时自动检查更新的提示

### 缺失功能

1. 多来源 Provider 抽象层。
2. 持久化的单文件更新来源绑定元数据。
3. 后台自动执行更新。
4. 全局自动应用策略。
5. CurseForge API Key 等特定来源配置。

## V1 更新来源矩阵

### V1 范围内

1. Modrinth

   * 检测方式：根据文件哈希自动识别
   * 更新方式：自动更新
2. CurseForge

   * 检测方式：通过官方指纹接口自动识别
   * 更新方式：自动更新
3. Hangar

   * 检测方式：手动绑定
   * 更新方式：绑定后自动更新
4. GitHub Releases

   * 检测方式：手动绑定
   * 更新方式：绑定后自动更新

### V1 范围外

1. Spiget

   * 原因：API 仍处于测试阶段，并采用间接同步模式
2. BuiltByBit、Polymart 和私有制品存储服务
3. 完全通用的自定义 URL Provider

## 为什么要拆分更新来源矩阵

Modrinth 和 CurseForge 可以通过本地文件指纹识别已安装文件。Hangar 和 GitHub 通常无法仅根据本地 JAR 文件可靠地推断其上游项目身份。

OPanel 当前只存储面向运行时的插件信息，例如文件名、显示名称、版本、作者和网站，并没有存储标准化的插件市场项目 ID。

因此，在 V1 中，Hangar 和 GitHub 需要用户显式绑定更新来源。

## 建议架构

### 新增核心组件

1. `UpdateSourceProvider`

   * 通用 Provider 接口
2. `PluginUpdateCoordinator`

   * 协调所有 Provider
   * 合并更新候选项
   * 应用全局更新策略
3. `PluginUpdateBinding`

   * 本地文件的持久化更新来源绑定
4. `PluginUpdateConfig`

   * 全局设置及绑定注册表
5. `PluginAutoUpdateService`

   * 定时后台执行服务

### Provider 列表

1. `ModrinthUpdateSourceProvider`
2. `CurseForgeUpdateSourceProvider`
3. `HangarUpdateSourceProvider`
4. `GitHubReleaseUpdateSourceProvider`

### 建议包路径

`core/src/main/java/net/opanel/update/`

## Provider 接口

建议职责：

1. 说明该 Provider 是否支持自动识别。
2. 识别已安装制品的身份。
3. 查找最新的兼容目标版本。
4. 返回标准化的更新候选项元数据。
5. 在可用时提供下载 URL 和完整性校验元数据。

建议的标准化输出字段：

1. 本地文件名
2. 来源类型
3. 来源项目标识符
4. 显示名称
5. 当前版本
6. 最新版本
7. 项目 URL
8. 下载 URL
9. 摘要算法和摘要值
10. 发布渠道
11. 是否需要手动绑定

## 持久化配置模型

### 全局配置新增项

向 `OPanelConfiguration` 添加：

1. `boolean autoApplyPluginUpdates`
2. `String pluginUpdateRestartStrategy`

   * `defer`
   * `restart-if-needed`
   * V1 默认值：`defer`
3. `String curseForgeApiKey`

保留：

1. `boolean autoCheckPluginUpdates`
2. `int pluginUpdateCheckInterval`

### 新增存储文件

添加新的存储键和文件：

1. `StorageKey.PLUGIN_UPDATE_CONFIG`
2. `opanel/plugin-update-config.json`

### 建议的 JSON 结构

```json
{
  "bindings": {
    "ViaVersion.jar": {
      "source": "hangar",
      "projectId": "ViaVersion",
      "owner": null,
      "repo": null,
      "assetPattern": null,
      "channels": ["release"]
    },
    "ExampleMod.jar": {
      "source": "github",
      "projectId": null,
      "owner": "example",
      "repo": "example-mod",
      "assetPattern": ".*\\.jar$",
      "channels": ["release"]
    }
  }
}
```

## 后台服务

### 行为

1. 每隔 `pluginUpdateCheckInterval` 秒运行一次。
2. 如果 `autoCheckPluginUpdates` 为 `false`，则跳过执行。
3. 通过 Coordinator 发现更新候选项。
4. 如果 `autoApplyPluginUpdates` 为 `false`：

   * 仅缓存更新结果
5. 如果 `autoApplyPluginUpdates` 为 `true`：

   * 仅应用通过验证的更新候选项
   * 保留当前的冲突检查
   * 保留摘要校验
   * 保留文件锁定时的延迟应用流程

### 安全规则

1. 同一时间只允许一个更新任务运行。
2. 忽略存在待处理启用、禁用或删除操作的文件。
3. 如果从发现更新到应用更新期间，已安装文件的哈希发生变化，则绝不覆盖该文件。
4. 当 Provider 提供可信摘要时，必须执行摘要校验。
5. 如果某个 Provider 在 V1 中无法提供稳定的完整性数据，则除非用户明确接受安全降级方案，否则不允许自动应用该来源的更新。

## 各更新来源的检测规则

### Modrinth

1. 使用当前基于哈希值匹配版本文件的流程。
2. 保留以下兼容性筛选条件：

   * 加载器或服务器类型
   * Minecraft 版本
   * 发布渠道

### CurseForge

1. 使用官方指纹匹配接口识别已安装文件。
2. 使用模组文件列表接口选择更新且兼容的文件。
3. 根据以下条件筛选：

   * 游戏版本
   * 模组加载器或平台标记（如适用）
   * 根据策略筛选发布类型
4. 需要配置 API Key。

### Hangar

1. 只检查已显式绑定的文件。
2. 根据 Hangar 项目短名称查询项目版本。
3. 根据以下条件筛选：

   * 平台，例如 `PAPER`、`VELOCITY`
   * Minecraft 版本支持情况
   * 发布渠道
4. 解析特定版本对应的下载接口。

### GitHub Releases

1. 只检查已显式绑定的文件。
2. 查询所绑定仓库的 Release。
3. 忽略草稿 Release。
4. 遵循发布渠道策略：

   * `release` 不包含预发布版本
   * `beta` 可以包含预发布版本
5. 根据配置的正则表达式选择 JAR 资源文件。
6. 如果 API 响应中提供了加密摘要，则优先使用该摘要。

## API 变更

### 保留现有接口

1. `POST /api/plugins/check-updates`
2. `POST /api/plugins/update`

### 新增接口

1. `GET /api/plugins/update-bindings`
2. `POST /api/plugins/update-bindings`
3. `GET /api/plugins/update-status`

### 更新后的 `check-updates` 响应结构

在现有更新记录中增加：

1. `source`
2. `projectId`
3. `requiresBinding`
4. `requiresRestart`
5. `channel`

## 前端 V1

### 插件页面变更

1. 为每个插件/模组显示来源标签：

   * `modrinth`
   * `curseforge`
   * `hangar`
   * `github`
   * `unbound`
2. 为未绑定的条目添加一个小型绑定对话框。
3. 保留当前更新对话框，并增加以下信息：

   * 更新来源
   * 目标版本
   * 是否需要重启或是否处于延迟应用状态

### 设置页面变更

添加一个简洁的更新策略区域：

1. 自动检查更新
2. 自动应用更新
3. 重启策略

V1 中不在前端暴露 CurseForge API Key。该 Key 仅存储在服务端。

## 更新应用流程

1. 发现更新候选项。
2. 验证候选项兼容性。
3. 验证当前文件自发现更新以来未发生变化。
4. 下载到临时文件。
5. 验证文件摘要。
6. 调用 `server.updatePlugin(...)`。
7. 如果更新被延迟：

   * 标记为等待重启
8. 使更新缓存失效。

## 失败模型

失败应以结构化状态呈现，而不是仅返回通用错误：

1. `UNBOUND_SOURCE`
2. `SOURCE_AUTH_REQUIRED`
3. `NO_COMPATIBLE_VERSION`
4. `DIGEST_MISSING`
5. `DIGEST_MISMATCH`
6. `INSTALLED_FILE_CHANGED`
7. `DOWNLOAD_FAILED`
8. `DEFERRED_UNTIL_RESTART`

## 实现顺序

1. 将当前 Modrinth 逻辑提取到 Provider 接口之后。
2. 添加标准化的更新候选模型和 Coordinator。
3. 添加持久化绑定及配置存储。
4. 集成 CurseForge Provider。
5. 集成 Hangar Provider。
6. 集成 GitHub Releases Provider。
7. 添加后台自动更新服务。
8. 扩展 API 响应。
9. 添加前端绑定和状态界面。

## 验证计划

### 自动化及代码级验证

1. 为每个 Provider 的版本筛选逻辑编写单元测试。
2. 为已安装文件发生变化时的冲突检测编写单元测试。
3. 为摘要校验失败流程编写单元测试。
4. 为延迟应用状态的传递流程编写单元测试。

### 手动验证

1. 仅使用 Modrinth 的现有更新流程仍可正常工作，行为保持不变。
2. 配置 API Key 后，可以识别并更新仅存在于 CurseForge 的制品。
3. 已绑定 Hangar 的插件能够检测到兼容更新。
4. 已绑定 GitHub 的插件能够检测到 Release 中匹配的 JAR 资源。
5. 更新已加载的插件/模组时，会回退到延迟应用机制，而不是直接失败。
6. 存在待处理启用、禁用或删除操作的文件不会被后台更新任务处理。

## 发布说明

V1 应使用较为保守的默认配置发布：

1. `autoCheckPluginUpdates = true`
2. `autoApplyPluginUpdates = false`
3. `pluginUpdateRestartStrategy = "defer"`

这样可以使系统行为与当前产品保持接近，同时允许用户在评审后主动启用后台自动应用更新。

## 外部参考资料

1. Modrinth API 文档：

   * `https://docs.modrinth.com/api/operations/getprojectversions/`
   * `https://docs.modrinth.com/api/operations/tags/version-files/`
2. CurseForge API 文档：

   * `https://docs.curseforge.com/rest-api/`
3. GitHub Releases API 文档：

   * `https://docs.github.com/en/rest/releases/releases`
4. Hangar 版本页面和公共项目 API 示例：

   * `https://hangar.papermc.io/CommandAPI/CommandAPI/versions`
