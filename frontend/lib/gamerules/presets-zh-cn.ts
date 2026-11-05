import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1487159&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1171974&variant=zh-cn
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 * @see https://zh.minecraft.wiki/w/游戏规则/allowFireTicksAwayFromPlayer?oldid=1529274&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则/disableElytraMovementCheck?oldid=1529275&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则/disablePlayerMovementCheck?oldid=1529276&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则/disableRaids?oldid=1529277&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则/doFireTick?oldid=1529278&variant=zh-cn
 * @see https://zh.minecraft.wiki/w/游戏规则/spawnChunkRadius?oldid=1194456&variant=zh-cn
 */
export const wikiUrl = "https://zh.minecraft.wiki/w/游戏规则?variant=zh-cn";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "游戏内时间流逝",
    description: ""
  },
  advance_weather: {
    name: "天气更替",
    description: ""
  },
  allow_entering_nether_using_portals: {
    name: "允许进入下界",
    description: "控制玩家能否进入下界。"
  },
  block_drops: {
    name: "方块掉落",
    description: "控制方块是否掉落资源，包括经验球。"
  },
  block_explosion_drop_decay: {
    name: "在方块交互爆炸中，一些方块不会掉落战利品",
    description: "在与方块交互引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  command_block_output: {
    name: "广播命令方块输出",
    description: ""
  },
  command_blocks_work: {
    name: "启用命令方块",
    description: ""
  },
  drowning_damage: {
    name: "溺水伤害",
    description: ""
  },
  elytra_movement_check: {
    name: "启用鞘翅移动检测",
    description: ""
  },
  ender_pearls_vanish_on_death: {
    name: "掷出的末影珍珠在死亡时消失",
    description: "玩家投掷的末影珍珠是否在玩家死亡时消失。"
  },
  entity_drops: {
    name: "非生物实体掉落",
    description: "控制各种矿车（含内容物）、物品展示框、船等的物品掉落。"
  },
  fall_damage: {
    name: "摔落伤害",
    description: ""
  },
  fire_damage: {
    name: "火焰伤害",
    description: ""
  },
  fire_spread_radius_around_player: {
    name: "火焰蔓延半径",
    description: "玩家周围火可以蔓延的方块半径。"
  },
  forgive_dead_players: {
    name: "宽恕死亡玩家",
    description: "愤怒的中立生物将在其目标玩家于附近死亡后息怒。"
  },
  freeze_damage: {
    name: "冰冻伤害",
    description: ""
  },
  global_sound_events: {
    name: "全局声音事件",
    description: "特定游戏事件（如Boss生成）发生时，声音可在所有地方听见。"
  },
  immediate_respawn: {
    name: "立即重生",
    description: ""
  },
  keep_inventory: {
    name: "死亡后保留物品栏",
    description: ""
  },
  lava_source_conversion: {
    name: "允许流动熔岩转化为熔岩源",
    description: "相邻两面方块为熔岩源的流动熔岩会转化为熔岩源。"
  },
  limited_crafting: {
    name: "合成需要配方",
    description: "若启用，玩家只能使用已解锁的配方合成。"
  },
  locator_bar: {
    name: "启用玩家定位栏",
    description: "启用后，屏幕上会显示指示玩家方位的定位栏。"
  },
  log_admin_commands: {
    name: "通告管理员命令",
    description: ""
  },
  max_block_modifications: {
    name: "命令修改方块数量限制",
    description: "单条命令（如fill和clone）最多能更改的方块数量。"
  },
  max_command_forks: {
    name: "命令上下文数量限制",
    description: "“execute as”等命令可以使用的上下文数量最大值。"
  },
  max_command_sequence_length: {
    name: "命令连锁执行数量限制",
    description: "应用于命令方块链和函数。"
  },
  max_entity_cramming: {
    name: "实体挤压上限",
    description: ""
  },
  max_minecart_speed: {
    name: "矿车最大速度",
    description: "矿车在地面上移动的默认最大速度。"
  },
  max_snow_accumulation_height: {
    name: "积雪厚度",
    description: "降雪时，地面上的雪最多堆积到此处指定的层数。"
  },
  mob_drops: {
    name: "生物战利品掉落",
    description: "控制生物是否掉落资源，包括经验球。"
  },
  mob_explosion_drop_decay: {
    name: "在生物爆炸中，一些方块不会掉落战利品",
    description: "在生物引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  mob_griefing: {
    name: "允许破坏性生物行为",
    description: ""
  },
  natural_health_regeneration: {
    name: "生命值自然恢复",
    description: ""
  },
  player_movement_check: {
    name: "启用玩家移动检测",
    description: ""
  },
  players_nether_portal_creative_delay: {
    name: "创造模式下玩家在下界传送门中等待的时间",
    description: "创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。"
  },
  players_nether_portal_default_delay: {
    name: "非创造模式下玩家在下界传送门中等待的时间",
    description: "非创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。"
  },
  players_sleeping_percentage: {
    name: "入睡占比",
    description: "跳过夜晚所需的入睡玩家占比。"
  },
  projectiles_can_break_blocks: {
    name: "弹射物能否破坏方块",
    description: "控制弹射物能否破坏可被其破坏的方块。"
  },
  pvp: {
    name: "启用PvP",
    description: "控制玩家间能否互相伤害。"
  },
  raids: {
    name: "启用袭击",
    description: ""
  },
  random_tick_speed: {
    name: "随机刻速率",
    description: ""
  },
  reduced_debug_info: {
    name: "简化调试信息",
    description: "限制调试屏幕内容。"
  },
  respawn_radius: {
    name: "重生点半径",
    description: "控制适合玩家生成的出生点周围区域大小。"
  },
  send_command_feedback: {
    name: "发送命令反馈",
    description: ""
  },
  show_advancement_messages: {
    name: "进度通知",
    description: ""
  },
  show_death_messages: {
    name: "显示死亡消息",
    description: ""
  },
  spawn_mobs: {
    name: "生成生物",
    description: "一些实体可能有其特定的规则。"
  },
  spawn_monsters: {
    name: "生成怪物",
    description: "控制怪物能否自然生成。"
  },
  spawn_patrols: {
    name: "生成灾厄巡逻队",
    description: ""
  },
  spawn_phantoms: {
    name: "生成幻翼",
    description: ""
  },
  spawn_wandering_traders: {
    name: "生成流浪商人",
    description: ""
  },
  spawn_wardens: {
    name: "生成监守者",
    description: ""
  },
  spawner_blocks_work: {
    name: "启用刷怪笼方块",
    description: ""
  },
  spectators_generate_chunks: {
    name: "允许旁观者生成地形",
    description: ""
  },
  spread_vines: {
    name: "藤蔓蔓延",
    description: "控制藤蔓方块能否随机向相邻的方块蔓延。不会影响其他藤蔓类方块（例如垂泪藤和缠怨藤等）。"
  },
  tnt_explodes: {
    name: "允许TNT被点燃并爆炸",
    description: ""
  },
  tnt_explosion_drop_decay: {
    name: "在TNT爆炸中，一些方块不会掉落战利品",
    description: "在TNT引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  universal_anger: {
    name: "无差别愤怒",
    description: "愤怒的中立生物将攻击附近的所有玩家，而不再限于激怒它们的玩家。禁用forgive_dead_players可达到最佳效果。"
  },
  water_source_conversion: {
    name: "允许流动水转化为水源",
    description: "相邻两面方块为水源的流动水会转化为水源。"
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "允许火在远离玩家处蔓延",
    description: "控制火和熔岩能否在距离任何玩家超过8个区块处蔓延。"
  },
  allowEnteringNetherUsingPortals: {
    name: "允许进入下界",
    description: "控制玩家能否进入下界。"
  },
  announceAdvancements: {
    name: "进度通知",
    description: ""
  },
  blockExplosionDropDecay: {
    name: "在方块交互爆炸中，一些方块不会掉落战利品",
    description: "在与方块交互引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  commandBlockOutput: {
    name: "广播命令方块输出",
    description: ""
  },
  commandBlocksEnabled: {
    name: "启用命令方块",
    description: ""
  },
  commandModificationBlockLimit: {
    name: "命令修改方块数量限制",
    description: "单条命令（如fill和clone）最多能更改的方块数量。"
  },
  disableElytraMovementCheck: {
    name: "禁用鞘翅移动检测",
    description: ""
  },
  disablePlayerMovementCheck: {
    name: "禁用玩家移动检测",
    description: ""
  },
  disableRaids: {
    name: "禁用袭击",
    description: ""
  },
  doDaylightCycle: {
    name: "游戏内时间流逝",
    description: ""
  },
  doEntityDrops: {
    name: "非生物实体掉落",
    description: "控制各种矿车（含内容物）、物品展示框、船等的物品掉落。"
  },
  doFireTick: {
    name: "火焰蔓延",
    description: ""
  },
  doImmediateRespawn: {
    name: "立即重生",
    description: ""
  },
  doInsomnia: {
    name: "生成幻翼",
    description: ""
  },
  doLimitedCrafting: {
    name: "合成需要配方",
    description: "若启用，玩家只能使用已解锁的配方合成。"
  },
  doMobLoot: {
    name: "生物战利品掉落",
    description: "控制生物是否掉落资源，包括经验球。"
  },
  doMobSpawning: {
    name: "生成生物",
    description: "一些实体可能有其特定的规则。"
  },
  doPatrolSpawning: {
    name: "生成灾厄巡逻队",
    description: ""
  },
  doTileDrops: {
    name: "方块掉落",
    description: "控制方块是否掉落资源，包括经验球。"
  },
  doTraderSpawning: {
    name: "生成流浪商人",
    description: ""
  },
  doVinesSpread: {
    name: "藤蔓蔓延",
    description: "控制藤蔓方块能否随机向相邻的方块蔓延。不会影响其他藤蔓类方块（例如垂泪藤和缠怨藤等）。"
  },
  doWardenSpawning: {
    name: "生成监守者",
    description: ""
  },
  doWeatherCycle: {
    name: "天气更替",
    description: ""
  },
  drowningDamage: {
    name: "溺水伤害",
    description: ""
  },
  enderPearlsVanishOnDeath: {
    name: "掷出的末影珍珠在死亡时消失",
    description: "玩家投掷的末影珍珠是否在玩家死亡时消失。"
  },
  fallDamage: {
    name: "摔落伤害",
    description: ""
  },
  fireDamage: {
    name: "火焰伤害",
    description: ""
  },
  forgiveDeadPlayers: {
    name: "宽恕死亡玩家",
    description: "愤怒的中立生物将在其目标玩家于附近死亡后息怒。"
  },
  freezeDamage: {
    name: "冰冻伤害",
    description: ""
  },
  globalSoundEvents: {
    name: "全局声音事件",
    description: "特定游戏事件（如Boss生成）发生时，声音可在所有地方听见。"
  },
  keepInventory: {
    name: "死亡后保留物品栏",
    description: ""
  },
  lavaSourceConversion: {
    name: "允许流动熔岩转化为熔岩源",
    description: "相邻两面方块为熔岩源的流动熔岩会转化为熔岩源。"
  },
  locatorBar: {
    name: "启用玩家定位栏",
    description: "启用后，屏幕上会显示指示玩家方位的定位栏。"
  },
  logAdminCommands: {
    name: "通告管理员命令",
    description: ""
  },
  maxCommandChainLength: {
    name: "命令连锁执行数量限制",
    description: "应用于命令方块链和函数。"
  },
  maxCommandForkCount: {
    name: "命令上下文数量限制",
    description: "“execute as”等命令可以使用的上下文数量最大值。"
  },
  maxEntityCramming: {
    name: "实体挤压上限",
    description: ""
  },
  minecartMaxSpeed: {
    name: "矿车最大速度",
    description: "矿车在地面上移动的默认最大速度。"
  },
  mobExplosionDropDecay: {
    name: "在生物爆炸中，一些方块不会掉落战利品",
    description: "在生物引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  mobGriefing: {
    name: "允许破坏性生物行为",
    description: ""
  },
  naturalRegeneration: {
    name: "生命值自然恢复",
    description: ""
  },
  playersNetherPortalCreativeDelay: {
    name: "创造模式下玩家在下界传送门中等待的时间",
    description: "创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。"
  },
  playersNetherPortalDefaultDelay: {
    name: "非创造模式下玩家在下界传送门中等待的时间",
    description: "非创造模式下的玩家通过下界传送门前往其他维度前需要站在其中等待的时间（以刻为单位）。"
  },
  playersSleepingPercentage: {
    name: "入睡占比",
    description: "跳过夜晚所需的入睡玩家占比。"
  },
  projectilesCanBreakBlocks: {
    name: "弹射物能否破坏方块",
    description: "控制弹射物能否破坏可被其破坏的方块。"
  },
  randomTickSpeed: {
    name: "随机刻速率",
    description: ""
  },
  reducedDebugInfo: {
    name: "简化调试信息",
    description: "限制调试屏幕内容。"
  },
  sendCommandFeedback: {
    name: "发送命令反馈",
    description: ""
  },
  showDeathMessages: {
    name: "显示死亡消息",
    description: ""
  },
  snowAccumulationHeight: {
    name: "积雪厚度",
    description: "降雪时，地面上的雪最多堆积到此处指定的层数。"
  },
  spawnChunkRadius: {
    name: "出生区块半径",
    description: "主世界出生点周围保持加载的区块数量。"
  },
  spawnMonsters: {
    name: "生成怪物",
    description: "控制怪物能否自然生成。"
  },
  spawnerBlocksEnabled: {
    name: "启用刷怪笼方块",
    description: ""
  },
  spawnRadius: {
    name: "重生点半径",
    description: "控制适合玩家生成的出生点周围区域大小。"
  },
  spectatorsGenerateChunks: {
    name: "允许旁观者生成地形",
    description: ""
  },
  tntExplodes: {
    name: "允许TNT被点燃并爆炸",
    description: ""
  },
  tntExplosionDropDecay: {
    name: "在TNT爆炸中，一些方块不会掉落战利品",
    description: "在TNT引起的爆炸中，部分被破坏方块的掉落物会被炸毁。"
  },
  universalAnger: {
    name: "无差别愤怒",
    description: "愤怒的中立生物将攻击附近的所有玩家，而不再限于激怒它们的玩家。禁用forgive_dead_players可达到最佳效果。"
  },
  waterSourceConversion: {
    name: "允许流动水转化为水源",
    description: "相邻两面方块为水源的流动水会转化为水源。"
  },
};

export default gamerulePresets;
