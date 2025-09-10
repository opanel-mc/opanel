<div align="center">

<img src="./images/brand.svg" width="300"/>

<br>
<br>

[![test](https://img.shields.io/github/actions/workflow/status/nocpiun/opanel/build.yml)](https://github.com/nocpiun/opanel/actions/workflows/build.yml)
[![LICENSE](https://img.shields.io/badge/license-MPL_2.0-blue.svg "LICENSE")](./LICENSE)
[![Stars](https://img.shields.io/github/stars/nocpiun/opanel.svg?label=Stars)](https://github.com/nocpiun/opanel/stargazers)

> Minecraft 服务器管理面板

**Language / 语言**: [English](./README-en.md) | [中文](#中文)

</div>

---

## 中文

### 项目描述

OPanel 是一个为 Minecraft 服务器管理员设计的管理面板，它以服务端插件的形式运行，支持 Bukkit、Spigot、Paper、Fabric、Forge 和 NeoForge 服务器。通过 Web 面板，您可以以更可靠、直观和简单的方式管理您的服务器！

### 功能特性

OPanel 的功能包括：

- 仪表板，提供服务器的全面概览
- 世界存档管理器，帮助您通过简单的界面轻松上传、下载、删除或启用世界存档
- 玩家管理器，帮助您管理玩家、封禁玩家和白名单，并执行踢出、封禁或更改权限等操作
- 游戏规则编辑器，帮助您无需输入任何命令即可切换游戏规则
- 插件管理器（计划中）
- 服务器终端，可以直接从 Web 面板发送消息或执行命令
- 服务器日志管理器和查看器

### 支持的平台和版本

支持以下目标平台和 Minecraft 版本：

|服务器类型|版本|
|---|---|
|Bukkit / Spigot / Paper |>=1.21|
|Fabric |>=1.21|
|Forge |1.21-1.21.1, >=1.21.3|
|NeoForge |>=1.21.5|

### 截图

![preview-dashboard](./images/preview-dashboard.png)

![preview-players](./images/preview-players.png)

![preview-terminal](./images/preview-terminal.png)

### 使用方法

只需从 [releases](https://github.com/nocpiun/opanel/releases) 下载对应的 jar 文件，并将其拖入您的 plugins / mods 文件夹。启动服务器后，您可以通过 `localhost:3000` 访问 Web 面板。

### 配置

您可以通过在服务器文件夹内生成的配置文件来配置 OPanel。

- `accessKey`: 用于访问 Web 面板的访问密钥。（默认值: "123456"）
- `salt`: 生成令牌时使用的数值。（默认值: "opanel"）
- `webServerPort`: Web 面板的端口。（默认值: 3000）

### 贡献

查看 [贡献指南](./CONTRIBUTING.md) 了解更多信息。

### 许可证

[MPL-2.0](./LICENSE)
