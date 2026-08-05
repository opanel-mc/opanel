# OPanel

OPanel是一个支持多个游戏版本的Minecraft服务端模组/插件，让服务器管理员通过插件启动的网页面板即可管理和操作服务器。

## 技术栈

- 后端：Javalin
- 前端：Next.js、React、Typescript、Shadcn UI、Tailwind CSS

## 项目结构

由于OPanel需要支持多个服务端的不同游戏版本，而不同服务端平台、甚至同一服务端平台的不同版本对同一功能的实现各不相同，所以本项目采用了多模块的架构，将与游戏本体无关的业务逻辑全部封装到core模块中，然后为每一个特定服务端的特定版本分别创建一个独立的模块，在这些模块中具体实现core模块提供的接口(interface)，这样一来，业务逻辑只需使用接口来调用具体功能即可。

在这些模块中，有几个模块名称中没有标注游戏版本号，而是以`-helper`结尾，这些模块中存放的是特定服务端的通用代码，以供特定服务端不同游戏版本的模块调用，减少重复的冗余代码。

除`core`与`frontend`外，所有服务端实现模块按平台归档到根目录下对应的平台文件夹中（`fabric/`、`forge/`、`neoforge/`、`paper/`），其中Paper与Folia因共用`paper-helper`而同归于`paper/`。注意各模块在Gradle中仍使用扁平的项目名（如`:fabric-1.21`），平台文件夹仅是物理目录归档，模块间通过`project(":...")`互相引用时无需带平台前缀。

```
OPanel
├─ .github/                                # GitHub 工作流与仓库配置
├─ core/                                   # 核心业务逻辑模块
│  └─ src/main/
│         ├─ java/
│         │  └─ net/opanel/
│         │     ├─ annotation/              # 自定义注解定义
│         │     ├─ common/                  # 核心领域模型与通用常量/接口
│         │     │  └─ features/             # 面板功能特性声明与开关能力
│         │     ├─ config/                  # 配置对象与配置管理
│         │     ├─ controller/              # HTTP 控制器基类与请求处理流程
│         │     │  ├─ api/                  # 面板内部管理 API
│         │     │  └─ openapi/              # 对外开放 API
│         │     ├─ endpoint/                # WebSocket 通信端点与消息协议
│         │     ├─ event/                   # 对接游戏内事件的事件系统与事件类型
│         │     ├─ logger/                  # 日志能力抽象
│         │     ├─ storage/                 # 存储抽象与存储键/文件定义
│         │     ├─ task/                    # 定时任务与调度管理
│         │     ├─ map/                     # 网页地图预渲染
│         │     ├─ terminal/                # 控制台日志监听与终端能力
│         │     ├─ time/                    # 运行时间/TPS 等时间状态模型
│         │     ├─ utils/                   # 通用工具类
│         │     └─ web/                     # Web 服务与 JWT 鉴权
│         └─ resources/                     # 前端构建产物（将一同打包到最终构建的jar包中）
│
├─ frontend/                               # 网页面板前端工程（Next.js+Typescript）
│  ├─ app/                                 # 页面路由与页面级组件
│  ├─ assets/                              # 字体/图片/Minecraft 静态素材
│  ├─ components/                          # 可复用组件与 UI 组件
│  ├─ contexts/                            # React Context 全局状态上下文
│  ├─ hooks/                               # 自定义 Hooks（鉴权、WS、交互等）
│  ├─ lang/                                # 国际化语言包与 i18n 配置
│  ├─ lib/                                 # 前端基础库与业务工具
│  │  ├─ formatting-codes/                 # Minecraft 文本格式化代码处理
│  │  ├─ gamerules/                        # Minecraft 游戏规则相关内容
│  │  ├─ nbt/                              # NBT 数据解析与处理
│  │  ├─ server-config/                    # Minecraft server.properties相关内容
│  │  ├─ ws/                               # WebSocket 协议与连接封装
│  │  ├─ map/                              # 网页地图渲染 Worker
│  │  ├─ api.ts                            # 后端 API 调用封装
│  │  ├─ emitter.ts                        # 全局单例的 EventEmitter（有且仅有一个`refresh-data`事件，用于简便地触发数据刷新，重新从服务端获取数据）
│  │  ├─ fonts.ts                          # 加载字体文件
│  │  ├─ global.ts                         # 版本与版权信息
│  │  ├─ i18n.ts                           # 加载和读取国际化语言包
│  │  ├─ settings.ts                       # 设置选项的加载与读取封装
│  │  ├─ texture.ts                        # 加载和读取 Minecraft 纹理贴图（用于背包管理功能显示物品贴图）
│  │  ├─ time.ts                           # 时间相关工具
│  │  ├─ types.ts                          # 类型定义
│  │  └─ utils.ts                          # 工具函数集合
│  ├─ wasm-lib/                            # Rust Wasm库（方块纹理数据解析、地图颜色对照表生成、网页地图着色）
│  ├─ public/                              # 公共静态资源目录
│  ├─ scripts/                             # 构建/预处理脚本
│  └─ style/                               # 全局样式与主题样式
│
├─ fabric/                                 # Fabric 平台模块
│  ├─ fabric-helper/                       # Fabric 公共实现（1.21.11及以下）
│  ├─ fabric-helper-unmapped/              # Fabric 公共实现（26.1及以上）
│  └─ fabric-<mc_version>/                 # Fabric 版本实现
├─ forge/                                  # Forge 平台模块
│  ├─ forge-helper/                        # Forge 公共实现
│  └─ forge-<mc_version>/                  # Forge 版本实现
├─ neoforge/                               # NeoForge 平台模块
│  ├─ neoforge-helper/                     # NeoForge 公共实现
│  └─ neoforge-<mc_version>/               # NeoForge 版本实现
├─ paper/                                  # Paper 系（Paper/Leaves/Folia）平台模块
│  ├─ paper-helper/                        # Paper/Leaves/Folia 公共实现
│  ├─ paper-<mc_version>/                  # Paper 版本实现
│  └─ folia-<mc_version>/                  # Folia 版本实现
└─ ...
```

## 项目规范

### 命名规范

- **kebab-case**: 前端工程中的文件夹与文件名称、java模块名称、i18n键名、前端设置选项键名
- **snake_case**: java包名（在标注Minecraft版本时使用snake_case）、全局常量（大写且snake_case）
- **camelCase**: 变量名、http和WebSocket传输的json对象中的键名
- **PascalCase**: 类名

### 代码规范

- 对于前端部分，请查看`/frontend/.oxlintrc.json`
- 对于后端与游戏具体实现部分，与前后代码风格一致即可

### i18n 国际化文案

#### 命名规则

i18n 键名使用 **kebab-case**，遵循以下格式：

```
<page>.[...<component>].<part>
```

- `<page>`: 页面名称（独立组件使用的通用文案可省略，使用 `common` 作为前缀）
- `[...<component>]`: 组件名称，可按层级从上到下嵌套
- `<part>`: 组件部分的名称，如 `placeholder`、`description`、`tooltip`、`loading` 等

特殊规则：
- 浏览器标签页标题的 id 应仅为页面名称（如 `"about": "About"`）
- 错误消息的 id 应使用 `error` 作为组件名（如 `common.error.400`）
- 通用文案（跨页面/组件共享）使用 `common.` 前缀，如 `common.copy.success`

详见 `/frontend/lang/README.md`

#### 分类和顺序

语言文件中的键应按以下顺序组织：

1. `$lang` - 语言名称标识
2. `common.*` - 通用文案（按子分类排序：error、gamemode、difficulty、copy、controls 等）
3. 各页面文案（按页面名称字母顺序排列：about、login、panel...）

同一分类内的键应按逻辑顺序排列（参考已有的文案排序），保持各语言文件结构一致。不同分类的i18n文案中间应用一个空行隔开。

### 其他

- Java的依赖若需要进行Shadow Jar的Relocate，必须Relocate到`net.opanel.deps.*`包下
- 编写对话框dialog时，必须单独新建xxx-dialog.tsx文件
- 使用DataTable组件，编写columns定义时，必须单独新建columns.tsx文件

## 前端单元测试

前端为单元测试提供了一些定制工具，参见`/frontend/test/test-helper.tsx`，在有需要的时候可以直接使用，而不是编写重复的冗余代码。

如果测试中包含对React组件的测试，那么需要在一开始就声明：
```ts
afterEach(() => cleanup());
```

由于文件加载顺序的问题，i18n方面的mock（见`/frontend/test/setup.ts`中对`@/lib/i18n`的mock）并不是100%生效。一般情况下，测试中还是直接使用`[i18n_id]`（mock过）的写法，如果因为组件在i18n被mock前被加载导致mock不生效，以致测试不通过，再改成正则表达式同时匹配`[i18n_id]`和实际中文文本的写法。可参考：`/frontend/app/panel/players/inventory/item-dialog.test.tsx`。

改完前端代码后，跑Oxlint和TS类型检查即可，不需要全量构建；改完后端代码后不需要执行Gradle构建。
