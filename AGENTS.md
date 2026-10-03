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
│         └─ resources/                     # 后端共享资源；前端由各版本模块构建并打包
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

构建流程与环境变量约定见 `/BUILDING.md`。各版本模块通过 `gradle.properties` 中的
`frontend_env_` 属性声明前端配置，构建时去掉前缀并转为大写环境变量。前端生成产物位于
各模块的 `build/frontend`，不得提交；Pumpkin 独立使用 `frontend/dist`。

## 项目规范

### 代码规范

- 对于前端部分，请查看`/frontend/.oxlintrc.json`
- 对于后端与游戏具体实现部分，与前后代码风格一致即可

### i18n 国际化文案

#### 命名规则

详见 `/frontend/lang/README.md`

#### 分类和顺序

同一分类内的键应按逻辑顺序排列（参考已有的文案排序），保持各语言文件结构一致。不同分类的i18n文案中间应用一个空行隔开。

### 其他

- 编写对话框dialog时，必须单独新建xxx-dialog.tsx文件
- 使用DataTable组件，编写columns定义时，必须单独新建columns.tsx文件

## 前端单元测试

前端为单元测试提供了一些定制工具，参见`/frontend/test/test-helper.tsx`，在有需要的时候可以直接使用，而不是编写重复的冗余代码。

如果测试中包含对React组件的测试，那么需要在一开始就声明：
```ts
afterEach(() => cleanup());
```

由于文件加载顺序的问题，i18n方面的mock（见`/frontend/test/setup.ts`中对`@/lib/i18n`的mock）并不是100%生效。一般情况下，测试中还是直接使用`[i18n_id]`（mock过）的写法，如果因为组件在i18n被mock前被加载导致mock不生效，以致测试不通过，再改成正则表达式同时匹配`[i18n_id]`和实际中文文本的写法。可参考：`/frontend/app/panel/players/inventory/item-dialog.test.tsx`。

## 注意事项

### Pumpkin

- Pumpkin Rust 端的实现无需与 Java 端做到 100% 完全一致，但整体功能、行为和处理流程仍应优先参照 Java 实现。
- 遇到需要向用户确认的问题时，应先查阅并对照 Java 实现，明确两端的行为差异后再提问。
- 如果发现 Java 实现存在缺陷、隐患或明显不合理的行为，应先暂停并向用户确认，不要直接将缺陷照搬到 Rust 端。
- 避免过度工程化，只实现当前功能真正需要的抽象、保护措施和复杂度。
- 在具体业务代码中编写工具函数前，应先检查 `/pumpkin/utils/` 中是否已有可直接复用的实现；如果有，应直接复用。如果没有，应先判断该函数是否可能被其他代码使用；可能复用的工具函数应放入 `/pumpkin/utils/` 中，只有确认其仅服务于当前业务时，才可以放在对应的业务代码文件中。
- 只编写能够验证实际行为、边界条件或防止回归的有效测试，不编写没有实际价值的测试。
- 大部分场景下的文件读写无需实现原子化、并发锁或额外的竞态保护；默认参照 Java 实现采用直接读写，除非用户明确要求或对应 Java 实现本身采用了这些机制。

### 其他

- 改完前端代码后，跑Oxlint和TS类型检查即可，**不需要全量构建**。
- 改完Java代码后**不需要执行Gradle构建**。
