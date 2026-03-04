# OPanel

OPanel是一个支持多个游戏版本的Minecraft服务端模组/插件，让服务器管理员通过插件启动的网页面板即可管理和操作服务器。

## 技术栈

- 后端：Javalin
- 前端：Next.js、React、Typescript、Shadcn UI、Tailwind CSS

## 项目结构

由于OPanel需要支持多个服务端的不同游戏版本，而不同服务端平台、甚至同一服务端平台的不同版本对同一功能的实现各不相同，所以本项目采用了多模块的架构，将与游戏本体无关的业务逻辑全部封装到core模块中，然后为每一个特定服务端的特定版本分别创建一个独立的模块，在这些模块中具体实现core模块提供的接口(interface)，这样一来，业务逻辑只需使用接口来调用具体功能即可。

在这些模块中，有几个模块名称中没有标注游戏版本号，而是以`-helper`结尾，这些模块中存放的是特定服务端的通用代码，以供特定服务端不同游戏版本的模块调用，减少重复的冗余代码。

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
│  ├─ public/                              # 公共静态资源目录
│  ├─ scripts/                             # 构建/预处理脚本
│  └─ style/                               # 全局样式与主题样式
│
├─ bukkit-helper/                          # Bukkit/Spigot/Paper/Folia 公共实现
├─ fabric-helper/                          # Fabric 公共实现
├─ forge-helper/                           # Forge 公共实现
├─ spigot-<mc_version>/                    # Bukkit 版本实现（注意：由于Bukkit模块依赖的是spigot的包，所以模块名称写spigot）
├─ folia-<mc_version>/                     # Folia 版本实现
├─ fabric-<mc_version>/                    # Fabric 版本实现
├─ forge-<mc_version>/                     # Forge 版本实现
├─ neoforge-<mc_version>/                  # NeoForge 版本实现
└─ ...
```

## 项目规范

### 命名规范

- **kebab-case**: 前端工程中的文件夹与文件名称、java模块名称、i18n键名、前端设置选项键名
- **snake_case**: java包名（在标注Minecraft版本时使用snake_case）、全局常量（大写且snake_case）
- **camelCase**: 变量名、http和WebSocket传输的json对象中的键名
- **PascalCase**: 类名

### 风格规范

- 对于`if` `for` `while` `switch`等关键字，后面不能跟空格，必须紧接左括号`(`，如：`if(...` `for(...`
- 禁止"换行+{+换行"：
```
// 错误
if(...)
{
  // ...
}

// 正确
if(...) {
  // ...
}
```
- 对于复杂的条件判断，采用if语句“提早退出”策略，且如果if的条件语句较短，可以将return直接跟在同一行结尾，不用写大括号，如：
```ts
useEffect(() => {
  if(!client) return; // return直接跟在同一行结尾
  // ...
}, [client, /*...*/]);
```

### 代码规范

- 对于前端部分，请查看`/frontend/eslint.config.mjs`，特别注意import语句的顺序
- 对于后端与游戏具体实现部分，与前后代码风格一致即可

### 其他

- Java的依赖若需要进行Shadow Jar的Relocate，必须Relocate到`net.opanel.deps.*`包下
- 编写对话框dialog时，必须单独新建xxx-dialog.tsx文件
- 使用DataTable组件，编写columns定义时，必须单独新建columns.tsx文件

## 启动 & 调试

### 前端

调试前端先需要事先启动一个装有OPanel（面板端口必须为`3000`）的Minecraft服务端实例。

```
cd frontend
npm run dev
```

**重要：禁止执行`npm run build`构建前端，这将会把前端构建产物复制到`/core/src/main/resources/web/`文件夹下，该文件夹将被提交到git中，容易造成代码冲突！**

### 后端

修改完代码后直接执行Gradle构建，将构建好的jar包放入对应版本的Minecraft服务端中，然后启动服务端。

## 前端单元测试

前端为单元测试提供了一些定制工具，参见`/frontend/test/test-helper.tsx`，在有需要的时候可以直接使用，而不是编写重复的冗余代码。

如果测试中包含对React组件的测试，那么需要在一开始就声明：
```ts
afterEach(() => cleanup());
```

由于文件加载顺序的问题，i18n方面的mock（见`/frontend/test/setup.ts`中对`@/lib/i18n`的mock）并不是100%生效。一般情况下，测试中还是直接使用`[i18n_id]`（mock过）的写法，如果因为组件在i18n被mock前被加载导致mock不生效，以致测试不通过，再改成正则表达式同时匹配`[i18n_id]`和实际中文文本的写法。可参考：`/frontend/app/panel/players/inventory/item-dialog.test.tsx`。
