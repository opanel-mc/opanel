import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1487159&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1171974&variant=zh-tw
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 * @see https://zh.minecraft.wiki/w/游戏规则/allowFireTicksAwayFromPlayer?oldid=1529274&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则/disableElytraMovementCheck?oldid=1529275&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则/disablePlayerMovementCheck?oldid=1529276&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则/disableRaids?oldid=1529277&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则/doFireTick?oldid=1529278&variant=zh-tw
 * @see https://zh.minecraft.wiki/w/游戏规则/spawnChunkRadius?oldid=1194456&variant=zh-tw
 */
export const wikiUrl = "https://zh.minecraft.wiki/w/游戏规则?variant=zh-tw";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "日夜交替",
    description: ""
  },
  advance_weather: {
    name: "更新天氣",
    description: ""
  },
  allow_entering_nether_using_portals: {
    name: "允許進入地獄",
    description: "控制玩家是否能進入地獄。"
  },
  block_drops: {
    name: "掉落方塊",
    description: "控制方塊資源掉落，包含經驗球。"
  },
  block_explosion_drop_decay: {
    name: "在與方塊互動的爆炸中，部分方塊不會掉落成戰利品",
    description: "在與方塊相互作用引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  command_block_output: {
    name: "記錄指令方塊輸出",
    description: ""
  },
  command_blocks_work: {
    name: "啟用指令方塊",
    description: ""
  },
  drowning_damage: {
    name: "造成溺水傷害",
    description: ""
  },
  elytra_movement_check: {
    name: "啟用鞘翅移動檢測",
    description: ""
  },
  ender_pearls_vanish_on_death: {
    name: "拋出的終界珍珠在死亡時消失",
    description: "玩家拋出的終界珍珠是否在玩家死亡時消失。"
  },
  entity_drops: {
    name: "掉落實體裝備",
    description: "控制礦車（包含內容物）、物品展示框、船等的物品掉落。"
  },
  fall_damage: {
    name: "造成摔落傷害",
    description: ""
  },
  fire_damage: {
    name: "造成火焰傷害",
    description: ""
  },
  fire_spread_radius_around_player: {
    name: "火焰蔓延半徑",
    description: "玩家周圍火焰可蔓延的半徑格數。"
  },
  forgive_dead_players: {
    name: "原諒死者",
    description: "當目標玩家在附近死亡後，被其激怒的中立生物將息怒。"
  },
  freeze_damage: {
    name: "造成冰凍傷害",
    description: ""
  },
  global_sound_events: {
    name: "全域聲音事件",
    description: "當特定遊戲事件發生時，例如生成 Boss，聲音可以在任何地方被聽見。"
  },
  immediate_respawn: {
    name: "立即重生",
    description: ""
  },
  keep_inventory: {
    name: "死亡後保留物品欄",
    description: ""
  },
  lava_source_conversion: {
    name: "流動熔岩轉化成熔岩源",
    description: "流動熔岩在被兩個熔岩源包圍時，會轉化成熔岩源。"
  },
  limited_crafting: {
    name: "需要配方才能合成",
    description: "若啟用，玩家將只能合成已解鎖的配方。"
  },
  locator_bar: {
    name: "啟用玩家定位條",
    description: "啟用後，畫面上會顯示指示玩家方位的定位條。"
  },
  log_admin_commands: {
    name: "記錄管理員指令",
    description: ""
  },
  max_block_modifications: {
    name: "指令修改方塊數量限制",
    description: "單個指令（例如 fill 或是 clone）最多能修改的方塊數量。"
  },
  max_command_forks: {
    name: "指令的上下文上限",
    description: "「execute as」等指令使用的上下文最大數量。"
  },
  max_command_sequence_length: {
    name: "指令連鎖大小限制",
    description: "套用於連鎖型指令方塊及函數。"
  },
  max_entity_cramming: {
    name: "實體擠壓上限",
    description: ""
  },
  max_minecart_speed: {
    name: "礦車最大速度",
    description: "礦車在地面上移動的預設速度上限。"
  },
  max_snow_accumulation_height: {
    name: "積雪厚度",
    description: "降雪時，地面上的積雪最多累積至此處指定的層數。"
  },
  mob_drops: {
    name: "掉落生物戰利品",
    description: "控制生物資源掉落，包含經驗球。"
  },
  mob_explosion_drop_decay: {
    name: "在生物的爆炸中，部分方塊不會掉落成戰利品",
    description: "在生物引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  mob_griefing: {
    name: "允許生物的破壞行為",
    description: ""
  },
  natural_health_regeneration: {
    name: "自然回血",
    description: ""
  },
  player_movement_check: {
    name: "啟用玩家移動檢測",
    description: ""
  },
  players_nether_portal_creative_delay: {
    name: "創造模式玩家使用地獄傳送門的等待時間",
    description: "創造模式玩家使用地獄傳送門前往其他維度前，需要站在其中的等待時間（以刻為單位）。"
  },
  players_nether_portal_default_delay: {
    name: "非創造模式玩家使用地獄傳送門的等待時間",
    description: "非創造模式玩家通過地獄傳送門前往其他維度前，需要站在其中的等待時間（以刻為單位）。"
  },
  players_sleeping_percentage: {
    name: "睡眠比例",
    description: "跳過夜晚所需的入睡玩家比例。"
  },
  projectiles_can_break_blocks: {
    name: "投射物是否能破壞方塊",
    description: "控制投射物是否能破壞可被其破壞的方塊。"
  },
  pvp: {
    name: "啟用 PvP",
    description: "控制玩家是否能互相傷害。"
  },
  raids: {
    name: "啟用突襲",
    description: ""
  },
  random_tick_speed: {
    name: "隨機刻速率",
    description: ""
  },
  reduced_debug_info: {
    name: "簡化除錯資訊",
    description: "限制除錯畫面的內容。"
  },
  respawn_radius: {
    name: "重生點半徑",
    description: "控制玩家能夠生成的重生點範圍區域大小。"
  },
  send_command_feedback: {
    name: "回傳指令回饋",
    description: ""
  },
  show_advancement_messages: {
    name: "進度通知",
    description: ""
  },
  show_death_messages: {
    name: "顯示死亡訊息",
    description: ""
  },
  spawn_mobs: {
    name: "生成生物",
    description: "部分實體可能採用不同的規則。"
  },
  spawn_monsters: {
    name: "生成怪物",
    description: "控制怪物是否能自然生成。"
  },
  spawn_patrols: {
    name: "生成掠奪者巡邏隊",
    description: ""
  },
  spawn_phantoms: {
    name: "生成夜魅",
    description: ""
  },
  spawn_wandering_traders: {
    name: "生成流浪商人",
    description: ""
  },
  spawn_wardens: {
    name: "生成伏守者",
    description: ""
  },
  spawner_blocks_work: {
    name: "啟用生怪磚",
    description: ""
  },
  spectators_generate_chunks: {
    name: "允許旁觀者生成地形",
    description: ""
  },
  spread_vines: {
    name: "藤蔓蔓延",
    description: "控制藤蔓方塊是否能隨機向相鄰的方塊蔓延。這不會影響其他類型的藤蔓方塊（例如垂泣藤及扭曲藤等）。"
  },
  tnt_explodes: {
    name: "允許 TNT 被點燃並爆炸",
    description: ""
  },
  tnt_explosion_drop_decay: {
    name: "在 TNT 的爆炸中，部分方塊不會掉落成戰利品",
    description: "在 TNT 引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  universal_anger: {
    name: "無差別憤怒",
    description: "被激怒的中立生物將攻擊附近的所有玩家，而不限於激怒它們的那位玩家。停用 forgive_dead_players 以達到最佳效果。"
  },
  water_source_conversion: {
    name: "流動水轉化成水源",
    description: "流動水在被兩個水源包圍時，會轉化成水源。"
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "允許火在遠離玩家處蔓延",
    description: "控制火和熔岩是否能在距離任何玩家 8 個區塊之外繼續蔓延。"
  },
  allowEnteringNetherUsingPortals: {
    name: "允許進入地獄",
    description: "控制玩家是否能進入地獄。"
  },
  announceAdvancements: {
    name: "進度通知",
    description: ""
  },
  blockExplosionDropDecay: {
    name: "在與方塊互動的爆炸中，部分方塊不會掉落成戰利品",
    description: "在與方塊相互作用引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  commandBlockOutput: {
    name: "記錄指令方塊輸出",
    description: ""
  },
  commandBlocksEnabled: {
    name: "啟用指令方塊",
    description: ""
  },
  commandModificationBlockLimit: {
    name: "指令修改方塊數量限制",
    description: "單個指令（例如 fill 或是 clone）最多能修改的方塊數量。"
  },
  disableElytraMovementCheck: {
    name: "停用鞘翅移動檢測",
    description: ""
  },
  disablePlayerMovementCheck: {
    name: "停用玩家移動檢測",
    description: ""
  },
  disableRaids: {
    name: "停用突襲",
    description: ""
  },
  doDaylightCycle: {
    name: "日夜交替",
    description: ""
  },
  doEntityDrops: {
    name: "掉落實體裝備",
    description: "控制礦車（包含內容物）、物品展示框、船等的物品掉落。"
  },
  doFireTick: {
    name: "更新火焰",
    description: ""
  },
  doImmediateRespawn: {
    name: "立即重生",
    description: ""
  },
  doInsomnia: {
    name: "生成夜魅",
    description: ""
  },
  doLimitedCrafting: {
    name: "需要配方才能合成",
    description: "若啟用，玩家將只能合成已解鎖的配方。"
  },
  doMobLoot: {
    name: "掉落生物戰利品",
    description: "控制生物資源掉落，包含經驗球。"
  },
  doMobSpawning: {
    name: "生成生物",
    description: "部分實體可能採用不同的規則。"
  },
  doPatrolSpawning: {
    name: "生成掠奪者巡邏隊",
    description: ""
  },
  doTileDrops: {
    name: "掉落方塊",
    description: "控制方塊資源掉落，包含經驗球。"
  },
  doTraderSpawning: {
    name: "生成流浪商人",
    description: ""
  },
  doVinesSpread: {
    name: "藤蔓蔓延",
    description: "控制藤蔓方塊是否能隨機向相鄰的方塊蔓延。這不會影響其他類型的藤蔓方塊（例如垂泣藤及扭曲藤等）。"
  },
  doWardenSpawning: {
    name: "生成伏守者",
    description: ""
  },
  doWeatherCycle: {
    name: "更新天氣",
    description: ""
  },
  drowningDamage: {
    name: "造成溺水傷害",
    description: ""
  },
  enderPearlsVanishOnDeath: {
    name: "拋出的終界珍珠在死亡時消失",
    description: "玩家拋出的終界珍珠是否在玩家死亡時消失。"
  },
  fallDamage: {
    name: "造成摔落傷害",
    description: ""
  },
  fireDamage: {
    name: "造成火焰傷害",
    description: ""
  },
  forgiveDeadPlayers: {
    name: "原諒死者",
    description: "當目標玩家在附近死亡後，被其激怒的中立生物將息怒。"
  },
  freezeDamage: {
    name: "造成冰凍傷害",
    description: ""
  },
  globalSoundEvents: {
    name: "全域聲音事件",
    description: "當特定遊戲事件發生時，例如生成 Boss，聲音可以在任何地方被聽見。"
  },
  keepInventory: {
    name: "死亡後保留物品欄",
    description: ""
  },
  lavaSourceConversion: {
    name: "流動熔岩轉化成熔岩源",
    description: "流動熔岩在被兩個熔岩源包圍時，會轉化成熔岩源。"
  },
  locatorBar: {
    name: "啟用玩家定位條",
    description: "啟用後，畫面上會顯示指示玩家方位的定位條。"
  },
  logAdminCommands: {
    name: "記錄管理員指令",
    description: ""
  },
  maxCommandChainLength: {
    name: "指令連鎖大小限制",
    description: "套用於連鎖型指令方塊及函數。"
  },
  maxCommandForkCount: {
    name: "指令的上下文上限",
    description: "「execute as」等指令使用的上下文最大數量。"
  },
  maxEntityCramming: {
    name: "實體擠壓上限",
    description: ""
  },
  minecartMaxSpeed: {
    name: "礦車最大速度",
    description: "礦車在地面上移動的預設速度上限。"
  },
  mobExplosionDropDecay: {
    name: "在生物的爆炸中，部分方塊不會掉落成戰利品",
    description: "在生物引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  mobGriefing: {
    name: "允許生物的破壞行為",
    description: ""
  },
  naturalRegeneration: {
    name: "自然回血",
    description: ""
  },
  playersNetherPortalCreativeDelay: {
    name: "創造模式玩家使用地獄傳送門的等待時間",
    description: "創造模式玩家使用地獄傳送門前往其他維度前，需要站在其中的等待時間（以刻為單位）。"
  },
  playersNetherPortalDefaultDelay: {
    name: "非創造模式玩家使用地獄傳送門的等待時間",
    description: "非創造模式玩家通過地獄傳送門前往其他維度前，需要站在其中的等待時間（以刻為單位）。"
  },
  playersSleepingPercentage: {
    name: "睡眠比例",
    description: "跳過夜晚所需的入睡玩家比例。"
  },
  projectilesCanBreakBlocks: {
    name: "投射物是否能破壞方塊",
    description: "控制投射物是否能破壞可被其破壞的方塊。"
  },
  randomTickSpeed: {
    name: "隨機刻速率",
    description: ""
  },
  reducedDebugInfo: {
    name: "簡化除錯資訊",
    description: "限制除錯畫面的內容。"
  },
  sendCommandFeedback: {
    name: "回傳指令回饋",
    description: ""
  },
  showDeathMessages: {
    name: "顯示死亡訊息",
    description: ""
  },
  snowAccumulationHeight: {
    name: "積雪厚度",
    description: "降雪時，地面上的積雪最多累積至此處指定的層數。"
  },
  spawnChunkRadius: {
    name: "重生區塊半徑",
    description: "主世界重生點周圍維持載入的區塊數量。"
  },
  spawnMonsters: {
    name: "生成怪物",
    description: "控制怪物是否能自然生成。"
  },
  spawnerBlocksEnabled: {
    name: "啟用生怪磚",
    description: ""
  },
  spawnRadius: {
    name: "重生點半徑",
    description: "控制玩家能夠生成的重生點範圍區域大小。"
  },
  spectatorsGenerateChunks: {
    name: "允許旁觀者生成地形",
    description: ""
  },
  tntExplodes: {
    name: "允許 TNT 被點燃並爆炸",
    description: ""
  },
  tntExplosionDropDecay: {
    name: "在 TNT 的爆炸中，部分方塊不會掉落成戰利品",
    description: "在 TNT 引發的爆炸中，會遺失部分被摧毀方塊的掉落物。"
  },
  universalAnger: {
    name: "無差別憤怒",
    description: "被激怒的中立生物將攻擊附近的所有玩家，而不限於激怒它們的那位玩家。停用 forgive_dead_players 以達到最佳效果。"
  },
  waterSourceConversion: {
    name: "流動水轉化成水源",
    description: "流動水在被兩個水源包圍時，會轉化成水源。"
  },
};

export default gamerulePresets;
