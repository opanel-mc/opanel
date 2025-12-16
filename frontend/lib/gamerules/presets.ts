import type { Gamerule } from ".";
import { Axe, BedDouble, Bomb, BookMarked, BowArrow, Boxes, BugOff, Cat, Clock, ClockFading, CloudSunRain, Flame, Gauge, GitBranch, HandCoins, Handshake, HeartMinus, HeartPlus, Hourglass, Link, MapPin, Megaphone, Minimize2, MountainSnow, Pickaxe, Radius, Settings, ShieldCheck, Skull, Snowflake, Sprout, Sword, Swords, Trophy, Volume2, Waves, WavesLadder } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/游戏规则#游戏规则列表 */
const gamerulePresets: Gamerule[] = [
  {
    id: "advance_time",
    name: "游戏内时间流逝",
    type: "boolean",
    icon: ClockFading
  },
  {
    id: "advance_weather",
    name: "天气更替",
    type: "boolean",
    icon: CloudSunRain
  },
  {
    id: "allow_entering_nether_using_portals",
    name: "允许进入下界	",
    description: "控制玩家能否进入下界",
    type: "boolean"
  },
  {
    id: "block_drops",
    name: "方块掉落",
    description: "控制破坏方块后是否掉落资源，包括经验球。",
    type: "boolean",
    icon: Pickaxe
  },
  {
    id: "block_explosion_drop_decay",
    name: "在方块交互爆炸中，一些方块不会掉落战利品",
    description: "在与方块交互引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "command_block_output",
    name: "广播命令方块输出",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "command_blocks_work",
    name: "启用命令方块",
    type: "boolean",
    icon: Settings
  },
  {
    id: "drowning_damage",
    name: "溺水伤害",
    type: "boolean",
    icon: WavesLadder
  },
  {
    id: "elytra_movement_check",
    name: "启用鞘翅移动检测",
    type: "boolean"
  },
  {
    id: "ender_pearls_vanish_on_death",
    name: "掷出的末影珍珠在死亡时消失",
    description: "玩家投掷的末影珍珠是否在玩家死亡时消失。",
    type: "boolean"
  },
  {
    id: "entity_drops",
    name: "非生物实体掉落",
    description: "控制矿车（包括内容物）、物品展示框、船等的物品掉落。",
    type: "boolean"
  },
  {
    id: "fall_damage",
    name: "摔落伤害",
    type: "boolean",
    icon: HeartMinus
  },
  {
    id: "fire_damage",
    name: "火焰伤害",
    type: "boolean",
    icon: Flame
  },
  {
    id: "fire_spread_radius_around_player",
    name: "火焰蔓延半径",
    description: "玩家周围火焰可以蔓延的方块半径",
    type: "number",
    icon: Radius
  },
  {
    id: "forgive_dead_players",
    name: "宽恕死亡玩家",
    description: "愤怒的中立生物将在其目标玩家于附近死亡后息怒。",
    type: "boolean",
    icon: Handshake
  },
  {
    id: "freeze_damage",
    name: "冰冻伤害",
    type: "boolean",
    icon: Snowflake
  },
  {
    id: "global_sound_events",
    name: "全局声音事件",
    description: "特定游戏事件（如Boss生成）发生时，声音可在所有地方听见。",
    type: "boolean",
    icon: Volume2
  },
  {
    id: "immediate_respawn",
    name: "立即重生",
    type: "boolean"
  },
  {
    id: "keep_inventory",
    name: "死亡后保留物品栏",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "lava_source_conversion",
    name: "允许流动熔岩转化为熔岩源",
    description: "流动熔岩在两面与熔岩源相邻时转化为熔岩源。",
    type: "boolean",
    icon: Waves
  },
  {
    id: "limited_crafting",
    name: "合成需要配方",
    description: "若启用，玩家只能使用已解锁的配方合成。",
    type: "boolean",
    icon: BookMarked
  },
  {
    id: "locator_bar",
    name: "启用玩家定位栏",
    description: "启用后，屏幕上会显示指示玩家方位的定位栏。",
    type: "boolean",
    icon: MapPin
  },
  {
    id: "log_admin_commands",
    name: "通告管理员命令",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "max_block_modifications",
    name: "命令修改方块数量限制",
    description: "单条命令（如fill和clone）最多能更改的方块数量。",
    type: "number",
    icon: Boxes
  },
  {
    id: "max_command_forks",
    name: "命令上下文数量限制",
    description: "“execute as”等命令可以使用的上下文数量最大值。",
    type: "number",
    icon: GitBranch
  },
  {
    id: "max_command_sequence_length",
    name: "命令连锁执行数量限制",
    description: "应用于命令方块链和函数。",
    type: "number",
    icon: Link
  },
  {
    id: "max_entity_cramming",
    name: "实体挤压上限",
    type: "number",
    icon: Minimize2
  },
  {
    id: "max_minecart_speed",
    name: "矿车最大速度",
    description: "矿车在地面上移动的默认最大速度。",
    type: "number",
    icon: Gauge
  },
  {
    id: "max_snow_accumulation_height",
    name: "积雪厚度",
    description: "降雪时，地面上的雪最多堆积到此处指定的层数。",
    type: "number",
    icon: Snowflake
  },
  {
    id: "mob_drops",
    name: "生物战利品掉落",
    description: "控制生物死亡后是否掉落资源，包括经验球。",
    type: "boolean",
    icon: Sword
  },
  {
    id: "mob_explosion_drop_decay",
    name: "在生物爆炸中，一些方块不会掉落战利品",
    description: "在生物引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "mob_griefing",
    name: "允许破坏性生物行为",
    type: "boolean",
  },
  {
    id: "natural_health_regeneration",
    name: "生命值自然恢复",
    type: "boolean",
    icon: HeartPlus
  },
  {
    id: "player_movement_check",
    name: "启用玩家移动检测",
    type: "boolean"
  },
  {
    id: "players_nether_portal_creative_delay",
    name: "创造模式下玩家在下界传送门中等待的时间",
    description: "创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。",
    type: "number",
    icon: Hourglass
  },
  {
    id: "players_nether_portal_default_delay",
    name: "非创造模式下玩家在下界传送门中等待的时间",
    description: "非创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。",
    type: "number",
    icon: Hourglass
  },
  {
    id: "players_sleeping_percentage",
    name: "入睡占比",
    description: "跳过夜晚所需的入睡玩家占比。",
    type: "number",
    icon: BedDouble
  },
  {
    id: "projectiles_can_break_blocks",
    name: "弹射物能否破坏方块",
    description: "控制弹射物是否能破坏可被其破坏的方块。",
    type: "boolean",
    icon: BowArrow
  },
  {
    id: "pvp",
    name: "启用PvP",
    description: "控制玩家间能否互相伤害",
    type: "boolean",
    icon: Swords
  },
  {
    id: "raids",
    name: "启用袭击",
    type: "boolean",
    icon: Axe
  },
  {
    id: "random_tick_speed",
    name: "随机刻速率",
    type: "number",
    icon: Clock
  },
  {
    id: "reduced_debug_info",
    name: "简化调试信息",
    description: "限制调试屏幕内容。",
    type: "boolean",
    icon: BugOff
  },
  {
    id: "respawn_radius",
    name: "重生点半径",
    description: "控制适合玩家生成的出生点周围区域大小。",
    type: "number",
    icon: Radius
  },
  {
    id: "send_command_feedback",
    name: "发送命令反馈",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "show_advancement_messages",
    name: "进度通知",
    type: "boolean",
    icon: Trophy
  },
  {
    id: "show_death_messages",
    name: "显示死亡消息",
    type: "boolean",
    icon: Skull
  },
  {
    id: "spawn_mobs",
    name: "生成生物",
    description: "一些实体可能有其特定的规则。",
    type: "boolean",
    icon: Cat
  },
  {
    id: "spawn_monsters",
    name: "生成怪物",
    description: "控制怪物能否自然生成",
    type: "boolean"
  },
  {
    id: "spawn_patrols",
    name: "生成灾厄巡逻队",
    type: "boolean",
    icon: Axe
  },
  {
    id: "spawn_phantoms",
    name: "生成幻翼",
    type: "boolean"
  },
  {
    id: "spawn_wandering_traders",
    name: "生成流浪商人",
    type: "boolean",
    icon: HandCoins
  },
  {
    id: "spawn_wardens",
    name: "生成监守者",
    type: "boolean",
    icon: Sword
  },
  {
    id: "spawner_blocks_work",
    name: "启用刷怪笼方块",
    type: "boolean"
  },
  {
    id: "spectators_generate_chunks",
    name: "允许旁观者生成地形",
    type: "boolean",
    icon: MountainSnow
  },
  {
    id: "spread_vines",
    name: "藤蔓蔓延",
    description: "控制藤蔓方块是否会随机向相邻的方块蔓延。不会影响其他藤蔓类方块（例如垂泪藤和缠怨藤等）。",
    type: "boolean",
    icon: Sprout
  },
  {
    id: "tnt_explodes",
    name: "允许TNT被点燃并爆炸",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "tnt_explosion_drop_decay",
    name: "在TNT爆炸中，一些方块不会掉落战利品",
    description: "在TNT引起的爆炸中，部分被破坏方块的掉落物会被炸毁。",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "universal_anger",
    name: "无差别愤怒",
    description: "愤怒的中立生物将攻击附近的所有玩家，而不再限于激怒它们的玩家。禁用“宽恕死亡玩家”可达到最佳效果。",
    type: "boolean",
    icon: Sword
  },
  {
    id: "water_source_conversion",
    name: "允许流动水转化为水源",
    description: "流动水在两面与水源相邻时转化为水源。",
    type: "boolean",
    icon: Waves
  }
];

export default gamerulePresets;
