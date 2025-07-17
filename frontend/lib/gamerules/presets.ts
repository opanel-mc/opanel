import { BedDouble, Bomb, BookMarked, BowArrow, Boxes, BugOff, Cat, Clock, ClockFading, CloudSunRain, Flame, Gauge, GitBranch, Handshake, HeartPlus, Hourglass, Link, MapPin, Megaphone, MountainSnow, Radius, ShieldUser, Snowflake, Swords, Trophy, Volume2, Waves, WavesLadder } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/%E6%B8%B8%E6%88%8F%E8%A7%84%E5%88%99#%E6%B8%B8%E6%88%8F%E8%A7%84%E5%88%99%E5%88%97%E8%A1%A8 */
const gamerulePresets = [
  {
    id: "allowFireTicksAwayFromPlayer",
    name: "允许火在远离玩家处蔓延",
    description: "控制火和熔岩是否能够在距离任何玩家超过8个区块处蔓延",
    type: "boolean",
    icon: Flame
  },
  {
    id: "announceAdvancements",
    name: "进度通知",
    type: "boolean",
    icon: Trophy
  },
  {
    id: "blockExplosionDropDecay",
    name: "在方块交互爆炸中，一些方块不会掉落战利品",
    description: "在与方块交互引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "commandBlockOutput",
    name: "广播命令方块输出",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "commandModificationBlockLimit",
    name: "命令修改方块数量限制",
    description: "单条命令（如fill和clone）最多能更改的方块数量",
    type: "number",
    icon: Boxes
  },
  {
    id: "disableElytraMovementCheck",
    name: "禁用鞘翅移动检测",
    type: "boolean"
  },
  {
    id: "disablePlayerMovementCheck",
    name: "禁用玩家移动检测",
    type: "boolean",
  },
  {
    id: "disableRaids",
    name: "禁用袭击",
    type: "boolean",
    icon: Swords
  },
  {
    id: "doDaylightCycle",
    name: "游戏内时间流逝",
    type: "boolean",
    icon: ClockFading
  },
  {
    id: "doEntityDrops",
    name: "非生物实体掉落",
    description: "控制矿车（包括内容物）、物品展示框、船等的物品掉落。",
    type: "boolean",
  },
  {
    id: "doFireTick",
    name: "火焰蔓延",
    type: "boolean",
    icon: Flame
  },
  {
    id: "doImmediateRespawn",
    name: "立即重生",
    type: "boolean",
  },
  {
    id: "doInsomnia",
    name: "生成幻翼",
    type: "boolean",
  },
  {
    id: "doLimitedCrafting",
    name: "合成需要配方",
    description: "若启用，玩家只能使用已解锁的配方合成。",
    type: "boolean",
    icon: BookMarked
  },
  {
    id: "doMobLoot",
    name: "生物战利品掉落",
    description: "控制生物死亡后是否掉落资源，包括经验球。",
    type: "boolean",
  },
  {
    id: "doMobSpawning",
    name: "生成生物",
    description: "一些实体可能有其特定的规则。",
    type: "boolean",
    icon: Cat
  },
  {
    id: "doPatrolSpawning",
    name: "生成灾厄巡逻队",
    type: "boolean",
  },
  {
    id: "doTileDrops",
    name: "方块掉落",
    description: "控制破坏方块后是否掉落资源，包括经验球。",
    type: "boolean",
  },
  {
    id: "doTraderSpawning",
    name: "生成流浪商人",
    type: "boolean",
  },
  {
    id: "doVinesSpread",
    name: "藤蔓蔓延",
    description: "控制藤蔓方块是否会随机向相邻的方块蔓延。不会影响其他藤蔓类方块（例如垂泪藤和缠怨藤等）。",
    type: "boolean",
  },
  {
    id: "doWardenSpawning",
    name: "生成监守者",
    type: "boolean",
  },
  {
    id: "doWeatherCycle",
    name: "天气更替",
    type: "boolean",
    icon: CloudSunRain
  },
  {
    id: "drowningDamage",
    name: "溺水伤害",
    type: "boolean",
    icon: WavesLadder
  },
  {
    id: "enderPearlsVanishOnDeath",
    name: "掷出的末影珍珠在死亡时消失",
    description: "玩家投掷的末影珍珠是否在玩家死亡时消失。",
    type: "boolean",
  },
  {
    id: "fallDamage",
    name: "摔落伤害",
    type: "boolean",
  },
  {
    id: "fireDamage",
    name: "火焰伤害",
    type: "boolean",
    icon: Flame
  },
  {
    id: "forgiveDeadPlayers",
    name: "宽恕死亡玩家",
    description: "愤怒的中立生物将在其目标玩家于附近死亡后息怒。",
    type: "boolean",
    icon: Handshake
  },
  {
    id: "freezeDamage",
    name: "冰冻伤害",
    type: "boolean",
    icon: Snowflake
  },
  {
    id: "globalSoundEvents",
    name: "全局声音事件",
    description: "特定游戏事件（如Boss生成）发生时，声音可在所有地方听见。",
    type: "boolean",
    icon: Volume2
  },
  {
    id: "keepInventory",
    name: "死亡后保留物品栏",
    type: "boolean",
  },
  {
    id: "lavaSourceConversion",
    name: "允许流动熔岩转化为熔岩源",
    description: "流动熔岩在两面与熔岩源相邻时转化为熔岩源。",
    type: "boolean",
  },
  {
    id: "locatorBar",
    name: "启用玩家定位栏",
    description: "启用后，屏幕上会显示指示玩家方位的定位栏。",
    type: "boolean",
    icon: MapPin
  },
  {
    id: "logAdminCommands",
    name: "通告管理员命令",
    type: "boolean",
    icon: ShieldUser
  },
  {
    id: "maxCommandChainLength",
    name: "命令连锁执行数量限制",
    description: "应用于命令方块链和函数。",
    type: "number",
    icon: Link
  },
  {
    id: "maxCommandForkCount",
    name: "命令上下文数量限制",
    description: "“execute as”等命令可以使用的上下文数量最大值。",
    type: "number",
    icon: GitBranch
  },
  {
    id: "maxEntityCramming",
    name: "实体挤压上限",
    type: "number",
  },
  {
    id: "minecartMaxSpeed",
    name: "矿车最大速度",
    description: "矿车在地面上移动的默认最大速度。",
    type: "number",
    icon: Gauge
  },
  {
    id: "mobExplosionDropDecay",
    name: "在生物爆炸中，一些方块不会掉落战利品",
    description: "在生物引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "mobGriefing",
    name: "允许破坏性生物行为",
    type: "boolean",
  },
  {
    id: "naturalRegeneration",
    name: "生命值自然恢复",
    type: "boolean",
    icon: HeartPlus
  },
  {
    id: "playersNetherPortalCreativeDelay",
    name: "创造模式下玩家在下界传送门中等待的时间",
    description: "创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。",
    type: "number",
    icon: Hourglass
  },
  {
    id: "playersNetherPortalDefaultDelay",
    name: "非创造模式下玩家在下界传送门中等待的时间",
    description: "非创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。",
    type: "number",
    icon: Hourglass
  },
  {
    id: "playersSleepingPercentage",
    name: "入睡占比",
    description: "跳过夜晚所需的入睡玩家占比。",
    type: "number",
    icon: BedDouble
  },
  {
    id: "projectilesCanBreakBlocks",
    name: "弹射物能否破坏方块",
    description: "控制弹射物是否能破坏可被其破坏的方块。",
    type: "boolean",
    icon: BowArrow
  },
  {
    id: "randomTickSpeed",
    name: "随机刻速率",
    type: "number",
    icon: Clock
  },
  {
    id: "reducedDebugInfo",
    name: "简化调试信息",
    description: "限制调试屏幕内容。",
    type: "boolean",
    icon: BugOff
  },
  {
    id: "sendCommandFeedback",
    name: "发送命令反馈",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "showDeathMessages",
    name: "显示死亡消息",
    type: "boolean",
  },
  {
    id: "snowAccumulationHeight",
    name: "积雪厚度",
    description: "降雪时，地面上的雪最多堆积到此处指定的层数。",
    type: "number",
    icon: Snowflake
  },
  {
    id: "spawnChunkRadius",
    name: "出生区块半径",
    description: "主世界出生点周围保持加载的区块数量。",
    type: "number",
    icon: Radius
  },
  {
    id: "spawnRadius",
    name: "重生点半径",
    description: "控制适合玩家生成的出生点周围区域大小。",
    type: "number",
    icon: Radius
  },
  {
    id: "spectatorsGenerateChunks",
    name: "允许旁观者生成地形",
    type: "boolean",
    icon: MountainSnow
  },
  {
    id: "tntExplodes",
    name: "允许TNT被点燃并爆炸",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "tntExplosionDropDecay",
    name: "在TNT爆炸中，一些方块不会掉落战利品",
    description: "在TNT引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "universalAnger",
    name: "无差别愤怒",
    description: "愤怒的中立生物将攻击附近的所有玩家，而不再限于激怒它们的玩家。禁用“宽恕死亡玩家”可达到最佳效果。",
    type: "boolean",
  },
  {
    id: "waterSourceConversion",
    name: "允许流动水转化为水源",
    description: "流动水在两面与水源相邻时转化为水源。",
    type: "boolean",
    icon: Waves
  }
];

export default gamerulePresets;
