import type { Property } from ".";
import { AppWindow, ArrowLeftRight, Bug, CirclePause, Contact, Earth, EthernetPort, FileArchive, FileKey, FilePen, HeartHandshake, KeyRound, Link, Link2, ListX, Lock, Megaphone, Package, PackageCheck, PackageOpen, PackageSearch, Plane, Radius, Server, Settings, ShieldCheck, Sprout, Square, SquareAsterisk, Swords, TentTree, Timer, Users, Waypoints } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/%E6%9C%8D%E5%8A%A1%E7%AB%AF%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F */
const serverPropertiesPresets: Property[] = [
  {
    id: "accepts-transfers",
    description: "允许服务端接受以Transfer数据包作为登录请求的传入连接",
    type: "boolean",
    icon: ArrowLeftRight
  },
  {
    id: "allow-flight",
    description: "允许玩家在安装添加飞行功能的mod前提下在生存模式下飞行",
    type: "boolean",
    icon: Plane
  },
  {
    id: "allow-nether",
    description: "是否开启地狱",
    type: "boolean"
  },
  {
    id: "broadcast-console-to-ops",
    description: "向所有在线OP发送所执行命令的输出",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "broadcast-rcon-to-ops",
    description: "向所有在线OP发送通过RCON执行的命令的输出",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "bug-report-link",
    description: "服务器“报告服务器漏洞”的URL",
    type: "string",
    icon: Link
  },
  {
    id: "debug",
    description: "启用服务器调试模式",
    type: "boolean",
    icon: Bug
  },
  {
    id: "difficulty",
    description: "定义服务器的游戏难度",
    type: "string"
  },
  {
    id: "enable-code-of-conduct",
    description: "是否启用行为准则功能",
    type: "boolean",
    icon: HeartHandshake
  },
  {
    id: "enable-command-block",
    description: "是否启用指令方块",
    type: "boolean",
    icon: Settings
  },
  {
    id: "enable-jmx-monitoring",
    description: "暴露特定的对象和属性用于暴露以毫秒为单位的tick时间",
    type: "boolean"
  },
  {
    id: "enable-query",
    description: "允许使用GameSpy4协议的服务器监听器，用于获取服务器信息",
    type: "boolean"
  },
  {
    id: "enable-rcon",
    description: "是否允许远程访问服务器控制台",
    type: "boolean",
    icon: Link2
  },
  {
    id: "enable-status",
    description: "使服务器在服务器列表中看起来是“在线”的",
    type: "boolean"
  },
  {
    id: "enforce-secure-profile",
    description: "要求玩家必须具有Mojang签名的公钥才能进入服务器",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "enforce-whitelist",
    description: "在服务器上强制执行白名单",
    type: "boolean",
    icon: Contact
  },
  {
    id: "entity-broadcast-range-percentage",
    description: "此选项控制实体需要距离玩家有多近才会将数据包发送给客户端",
    type: "number",
    icon: Radius
  },
  {
    id: "force-gamemode",
    description: "强制玩家加入时为默认游戏模式",
    type: "boolean"
  },
  {
    id: "function-permission-level",
    description: "设定函数解析时的权限等级",
    type: "number"
  },
  {
    id: "gamemode",
    description: "定义默认游戏模式",
    type: "string"
  },
  {
    id: "generate-structures",
    description: "定义是否能生成结构（例如村庄）",
    type: "boolean",
    icon: TentTree
  },
  {
    id: "generator-settings",
    description: "本属性质用于自定义世界的生成",
    type: "string",
    icon: Earth
  },
  {
    id: "hardcore",
    description: "如果设为true，服务器难度的设置会被忽略并且设为hard（困难），玩家在死后会自动切换至旁观模式",
    type: "boolean"
  },
  {
    id: "hide-online-players",
    description: "如果设为true，服务端在响应客户端状态请求时不会返回在线玩家列表",
    type: "boolean"
  },
  {
    id: "initial-disabled-packs",
    description: "需要在创建世界过程中禁用的数据包名称，以逗号分隔",
    type: "string",
    icon: Package
  },
  {
    id: "initial-enabled-packs",
    description: "需要在创建世界过程中启用的数据包名称，以逗号分隔",
    type: "string",
    icon: PackageOpen
  },
  {
    id: "level-name",
    description: "世界名称及其文件夹名",
    type: "string",
    icon: Earth
  },
  {
    id: "level-seed",
    description: "为你的世界定义一个种子",
    type: "string",
    icon: Sprout
  },
  {
    id: "level-type",
    description: "使用世界预设ID，确定地图所生成的类型",
    type: "string"
  },
  {
    id: "log-ips",
    description: "是否在有新玩家加入游戏时，在服务器日志中记录其IP地址",
    type: "boolean"
  },
  {
    id: "management-server-enabled",
    description: "用于控制是否启用Minecraft服务器管理协议",
    type: "boolean",
    icon: AppWindow
  },
  {
    id: "management-server-host",
    description: "用于设置Minecraft服务器管理协议监听的主机",
    type: "string",
    icon: Server
  },
  {
    id: "management-server-port",
    description: "用于设置Minecraft服务器管理协议监听的端口号",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "management-server-secret",
    description: "允许客户端提供包含服务器特定密钥的认证标头，该密钥是长度为40的字母及数字",
    type: "number",
    icon: SquareAsterisk
  },
  {
    id: "management-server-tls-enabled",
    description: "用于设置Minecraft服务器管理协议是否启用TLS",
    type: "boolean",
    icon: Lock
  },
  {
    id: "management-server-tls-keystore",
    description: "用于设置Minecraft服务器管理协议的TLS密钥库文件路径",
    type: "boolean",
    icon: FileKey
  },
  {
    id: "management-server-tls-keystore-password",
    description: "用于设置Minecraft服务器管理协议TLS密钥库文件的密码",
    type: "boolean",
    icon: KeyRound
  },
  {
    id: "max-build-height",
    description: "玩家在游戏中能够建造的最大高度",
    type: "number"
  },
  {
    id: "max-chained-neighbor-updates",
    description: "限制连锁NC更新的数量，超过此数量的连锁NC更新会被跳过",
    type: "number"
  },
  {
    id: "max-players",
    description: "服务器同时能容纳的最大玩家数量",
    type: "number",
    icon: Users
  },
  {
    id: "max-tick-time",
    description: "设置每个tick花费的最大毫秒数",
    type: "number",
    icon: Timer
  },
  {
    id: "max-world-size",
    description: "设置可让世界边界获得的最大半径值，单位为方块",
    type: "number",
    icon: Earth
  },
  // {
  //   id: "motd",
  //   description: "本属性值是玩家客户端的多人游戏服务器列表中显示的服务器信息，显示于名称下方",
  //   type: "string"
  // },
  {
    id: "network-compression-threshold",
    description: "网络压缩阈值，数据包大小超过该值时会进行压缩",
    type: "number",
    icon: FileArchive
  },
  {
    id: "online-mode",
    description: "是否让服务器对比Minecraft账户数据库验证登录信息",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "op-permission-level",
    description: "设定使用/op命令时添加管理员时的默认权限等级",
    type: "number"
  },
  {
    id: "pause-when-empty-seconds",
    description: "服务器在没有玩家在线指定秒数后自动停止计算",
    type: "number",
    icon: CirclePause
  },
  {
    id: "player-idle-timeout",
    description: "如果不为0，服务器将在玩家的空闲时间达到设置的时间（单位为分钟）时将玩家踢出服务器",
    type: "number"
  },
  {
    id: "prevent-proxy-connections",
    description: "若玩家加入服务器时使用的IP地址与连接验证服务器的不同，则会验证不通过",
    type: "boolean",
    icon: Waypoints
  },
  {
    id: "pvp",
    description: "是否允许PvP",
    type: "boolean",
    icon: Swords
  },
  {
    id: "query.port",
    description: "设置监听服务器的端口号",
    type: "number"
  },
  {
    id: "rate-limit",
    description: "设置玩家被踢出服务器前，可以发送的数据包数量",
    type: "number"
  },
  {
    id: "rcon.password",
    description: "设置RCON远程访问的密码",
    type: "string",
    icon: KeyRound
  },
  {
    id: "rcon.port",
    description: "设置RCON远程访问的端口号",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "region-file-compression",
    description: "设置区域文件压缩算法",
    type: "string",
    icon: FileArchive
  },
  {
    id: "require-resource-pack",
    description: "强制玩家启用服务器资源包",
    type: "boolean",
    icon: Package
  },
  {
    id: "resource-pack",
    description: "设置一个服务器资源包，玩家可选择是否使用该资源包",
    type: "string",
    icon: Package
  },
  {
    id: "resource-pack-id",
    description: "resource-pack指定的资源包的UUID，用于客户端标识资源包",
    type: "string",
    icon: PackageSearch
  },
  {
    id: "resource-pack-prompt",
    description: "用于在使用require-resource-pack时在资源包提示界面显示自定义信息",
    type: "string",
    icon: Package
  },
  {
    id: "resource-pack-sha1",
    description: "资源包的SHA-1值，必须为小写十六进制，用于验证资源包的完整性",
    type: "string",
    icon: PackageCheck
  },
  {
    id: "server-ip",
    description: "将服务器与一个特定IP绑定",
    type: "string",
    icon: Server
  },
  {
    id: "server-port",
    description: "服务器监听的端口号",
    type: "number",
    icon: EthernetPort
  },
  {
    id: "simulation-distance",
    description: "设置服务端可更新实体范围的最大值（模拟距离）",
    type: "number",
    icon: Radius
  },
  {
    id: "spawn-animals",
    description: "决定动物是否可以生成",
    type: "boolean"
  },
  {
    id: "spawn-monsters",
    description: "决定攻击型生物（怪物）是否可以生成",
    type: "boolean"
  },
  {
    id: "spawn-npcs",
    description: "决定是否生成村民",
    type: "boolean"
  },
  {
    id: "spawn-protection",
    description: "通过将该值进行2x+1的运算来决定出生点保护区域的边长",
    type: "number",
    icon: Square
  },
  {
    id: "status-heartbeat-interval",
    description: "控制管理服务器向已连接的客户端发送心跳通知的间隔",
    type: "number"
  },
  {
    id: "sync-chunk-writes",
    description: "启用后区块文件以同步模式写入",
    type: "boolean",
    icon: FilePen
  },
  {
    id: "text-filtering-config",
    description: "服务器中需要被屏蔽的文本（仅在Realms内部使用）",
    type: "string",
    icon: ListX
  },
  {
    id: "text-filtering-version",
    description: "服务器中需要被屏蔽文本格式的版本",
    type: "number",
    icon: ListX
  },
  {
    id: "use-native-transport",
    description: "是否使用针对Linux平台的数据包收发优化",
    type: "boolean",
    icon: ArrowLeftRight
  },
  {
    id: "view-distance",
    description: "设置服务端发送给客户端的世界数据量",
    type: "number",
    icon: Radius
  },
  {
    id: "white-list",
    description: "启用服务器的白名单",
    type: "boolean",
    icon: Contact
  }
];

export default serverPropertiesPresets;
