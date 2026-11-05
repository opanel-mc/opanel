import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1487159&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则?oldid=1171974&variant=zh-hk
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 * @see https://zh.minecraft.wiki/w/游戏规则/allowFireTicksAwayFromPlayer?oldid=1529274&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则/disableElytraMovementCheck?oldid=1529275&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则/disablePlayerMovementCheck?oldid=1529276&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则/disableRaids?oldid=1529277&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则/doFireTick?oldid=1529278&variant=zh-hk
 * @see https://zh.minecraft.wiki/w/游戏规则/spawnChunkRadius?oldid=1194456&variant=zh-hk
 */
export const wikiUrl = "https://zh.minecraft.wiki/w/游戏规则?variant=zh-hk";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "遊戲內時間推移",
    description: ""
  },
  advance_weather: {
    name: "更新天氣",
    description: ""
  },
  allow_entering_nether_using_portals: {
    name: "允許進入地獄",
    description: "控制玩家能否進入地獄。"
  },
  block_drops: {
    name: "跌落方塊",
    description: "控制方塊資源跌落，包含經驗球。"
  },
  block_explosion_drop_decay: {
    name: "方塊互動爆炸中，部分方塊不會跌落戰利品",
    description: "與方塊互動引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  command_block_output: {
    name: "記錄命令方塊輸出",
    description: ""
  },
  command_blocks_work: {
    name: "啟用命令方塊",
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
    name: "跌落實體裝備",
    description: "控制礦車（包括儲存物）、物品展示框、船之類的跌落物。"
  },
  fall_damage: {
    name: "造成跌落傷害",
    description: ""
  },
  fire_damage: {
    name: "造成火焰傷害",
    description: ""
  },
  fire_spread_radius_around_player: {
    name: "火焰蔓延半徑",
    description: "玩家周圍火焰可蔓延的半徑（以方塊為單位）。"
  },
  forgive_dead_players: {
    name: "原諒死亡玩家",
    description: "當目標玩家在附近死亡之後，激怒的中立生物將會息怒。"
  },
  freeze_damage: {
    name: "造成冰凍傷害",
    description: ""
  },
  global_sound_events: {
    name: "全局聲音事件",
    description: "特定遊戲事件（例如生成 Boss ）發生時，聲音可於所有地方聽見。"
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
    name: "容許流動熔巖轉化為熔巖源",
    description: "流動熔巖的兩面與熔巖源相鄰時，會轉化為熔巖源。"
  },
  limited_crafting: {
    name: "需要配方合成",
    description: "若啟用，玩家將僅可合成已解鎖的配方。"
  },
  locator_bar: {
    name: "啟用玩家定位條",
    description: "啟用後，畫面上會顯示定位條，以指示玩家方位。"
  },
  log_admin_commands: {
    name: "記錄管理員命令",
    description: ""
  },
  max_block_modifications: {
    name: "命令修改方塊數量限制",
    description: "單個命令（例如 fill 或 clone）最多可以修改的方塊數量。"
  },
  max_command_forks: {
    name: "命令上下文最大值",
    description: "「execute as」等命令可以使用的上下文數量最大值。"
  },
  max_command_sequence_length: {
    name: "命令連環執行數量限制",
    description: "套用於連鎖型命令方塊及函數。"
  },
  max_entity_cramming: {
    name: "實體擠壓上限",
    description: ""
  },
  max_minecart_speed: {
    name: "礦車最大速度",
    description: "礦車於地面移動的預設速度上限。"
  },
  max_snow_accumulation_height: {
    name: "積雪厚度",
    description: "落雪時，地面上的雪最多可以堆積到此處指定的層數。"
  },
  mob_drops: {
    name: "跌落生物戰利品",
    description: "控制生物資源跌落，包含經驗球。"
  },
  mob_explosion_drop_decay: {
    name: "生物爆炸中，部分方塊不會跌落戰利品",
    description: "生物引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  mob_griefing: {
    name: "允許生物破壞行為",
    description: ""
  },
  natural_health_regeneration: {
    name: "自然恢復生命值",
    description: ""
  },
  player_movement_check: {
    name: "啟用玩家移動檢測",
    description: ""
  },
  players_nether_portal_creative_delay: {
    name: "創造模式下玩家於地獄傳送門內的等待時間",
    description: "創造模式玩家變更維度前須站於地獄傳送門內的時間（以刻為單位）。"
  },
  players_nether_portal_default_delay: {
    name: "非創造模式下玩家於地獄傳送門內的等待時間",
    description: "非創造模式玩家變更維度前須站於地獄傳送門內的時間（以刻為單位）。"
  },
  players_sleeping_percentage: {
    name: "入睡百分比",
    description: "跳過夜晚所需的入睡玩家百分比。"
  },
  projectiles_can_break_blocks: {
    name: "彈射物破壞方塊",
    description: "控制彈射物是否能破壞可被其破壞的方塊。"
  },
  pvp: {
    name: "啟用 PvP",
    description: "控制玩家能否互相傷害。"
  },
  raids: {
    name: "啟用襲擊",
    description: ""
  },
  random_tick_speed: {
    name: "隨機刻速率",
    description: ""
  },
  reduced_debug_info: {
    name: "簡化除錯資訊",
    description: "限制除錯畫面內容。"
  },
  respawn_radius: {
    name: "重生點半徑",
    description: "控制玩家可以生成的重生點周圍區域大小。"
  },
  send_command_feedback: {
    name: "傳送命令執行資訊",
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
    description: "部分實體可能會有特殊規則。"
  },
  spawn_monsters: {
    name: "生成怪物",
    description: "控制怪物能否自然生成。"
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
    name: "生成沉靈守衞",
    description: ""
  },
  spawner_blocks_work: {
    name: "啟用生怪籠",
    description: ""
  },
  spectators_generate_chunks: {
    name: "允許旁觀者生成地形",
    description: ""
  },
  spread_vines: {
    name: "藤蔓蔓延",
    description: "控制藤蔓類方塊能否隨機向相鄰的方塊蔓延。此規則不會影響其他藤蔓類方塊（例如垂泣藤及扭曲藤等）。"
  },
  tnt_explodes: {
    name: "允許 TNT 被點燃並爆炸",
    description: ""
  },
  tnt_explosion_drop_decay: {
    name: "TNT 爆炸中，部分方塊不會跌落戰利品",
    description: "TNT 引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  universal_anger: {
    name: "無差別憤怒",
    description: "被激怒的中立生物會攻擊附近所有玩家，不僅限於激怒它們的玩家。停用「forgive_dead_players」效果更佳。"
  },
  water_source_conversion: {
    name: "容許流動水轉化為水源",
    description: "流動水的兩面與熔巖源相鄰時轉化為水源。"
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "允許火於遠離玩家處蔓延",
    description: "控制火和熔巖能否於距離任何玩家 8 個區塊之外。"
  },
  allowEnteringNetherUsingPortals: {
    name: "允許進入地獄",
    description: "控制玩家能否進入地獄。"
  },
  announceAdvancements: {
    name: "進度通知",
    description: ""
  },
  blockExplosionDropDecay: {
    name: "方塊互動爆炸中，部分方塊不會跌落戰利品",
    description: "與方塊互動引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  commandBlockOutput: {
    name: "記錄命令方塊輸出",
    description: ""
  },
  commandBlocksEnabled: {
    name: "啟用命令方塊",
    description: ""
  },
  commandModificationBlockLimit: {
    name: "命令修改方塊數量限制",
    description: "單個命令（例如 fill 或 clone）最多可以修改的方塊數量。"
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
    name: "停用襲擊",
    description: ""
  },
  doDaylightCycle: {
    name: "遊戲內時間推移",
    description: ""
  },
  doEntityDrops: {
    name: "跌落實體裝備",
    description: "控制礦車（包括儲存物）、物品展示框、船之類的跌落物。"
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
    name: "生成夜魅",
    description: ""
  },
  doLimitedCrafting: {
    name: "需要配方合成",
    description: "若啟用，玩家將僅可合成已解鎖的配方。"
  },
  doMobLoot: {
    name: "跌落生物戰利品",
    description: "控制生物資源跌落，包含經驗球。"
  },
  doMobSpawning: {
    name: "生成生物",
    description: "部分實體可能會有特殊規則。"
  },
  doPatrolSpawning: {
    name: "生成掠奪者巡邏隊",
    description: ""
  },
  doTileDrops: {
    name: "跌落方塊",
    description: "控制方塊資源跌落，包含經驗球。"
  },
  doTraderSpawning: {
    name: "生成流浪商人",
    description: ""
  },
  doVinesSpread: {
    name: "藤蔓蔓延",
    description: "控制藤蔓類方塊能否隨機向相鄰的方塊蔓延。此規則不會影響其他藤蔓類方塊（例如垂泣藤及扭曲藤等）。"
  },
  doWardenSpawning: {
    name: "生成沉靈守衞",
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
    name: "造成跌落傷害",
    description: ""
  },
  fireDamage: {
    name: "造成火焰傷害",
    description: ""
  },
  forgiveDeadPlayers: {
    name: "原諒死亡玩家",
    description: "當目標玩家在附近死亡之後，激怒的中立生物將會息怒。"
  },
  freezeDamage: {
    name: "造成冰凍傷害",
    description: ""
  },
  globalSoundEvents: {
    name: "全局聲音事件",
    description: "特定遊戲事件（例如生成 Boss ）發生時，聲音可於所有地方聽見。"
  },
  keepInventory: {
    name: "死亡後保留物品欄",
    description: ""
  },
  lavaSourceConversion: {
    name: "容許流動熔巖轉化為熔巖源",
    description: "流動熔巖的兩面與熔巖源相鄰時，會轉化為熔巖源。"
  },
  locatorBar: {
    name: "啟用玩家定位條",
    description: "啟用後，畫面上會顯示定位條，以指示玩家方位。"
  },
  logAdminCommands: {
    name: "記錄管理員命令",
    description: ""
  },
  maxCommandChainLength: {
    name: "命令連環執行數量限制",
    description: "套用於連鎖型命令方塊及函數。"
  },
  maxCommandForkCount: {
    name: "命令上下文最大值",
    description: "「execute as」等命令可以使用的上下文數量最大值。"
  },
  maxEntityCramming: {
    name: "實體擠壓上限",
    description: ""
  },
  minecartMaxSpeed: {
    name: "礦車最大速度",
    description: "礦車於地面移動的預設速度上限。"
  },
  mobExplosionDropDecay: {
    name: "生物爆炸中，部分方塊不會跌落戰利品",
    description: "生物引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  mobGriefing: {
    name: "允許生物破壞行為",
    description: ""
  },
  naturalRegeneration: {
    name: "自然恢復生命值",
    description: ""
  },
  playersNetherPortalCreativeDelay: {
    name: "創造模式下玩家於地獄傳送門內的等待時間",
    description: "創造模式玩家變更維度前須站於地獄傳送門內的時間（以刻為單位）。"
  },
  playersNetherPortalDefaultDelay: {
    name: "非創造模式下玩家於地獄傳送門內的等待時間",
    description: "非創造模式玩家變更維度前須站於地獄傳送門內的時間（以刻為單位）。"
  },
  playersSleepingPercentage: {
    name: "入睡百分比",
    description: "跳過夜晚所需的入睡玩家百分比。"
  },
  projectilesCanBreakBlocks: {
    name: "彈射物破壞方塊",
    description: "控制彈射物是否能破壞可被其破壞的方塊。"
  },
  randomTickSpeed: {
    name: "隨機刻速率",
    description: ""
  },
  reducedDebugInfo: {
    name: "簡化除錯資訊",
    description: "限制除錯畫面內容。"
  },
  sendCommandFeedback: {
    name: "傳送命令執行資訊",
    description: ""
  },
  showDeathMessages: {
    name: "顯示死亡訊息",
    description: ""
  },
  snowAccumulationHeight: {
    name: "積雪厚度",
    description: "落雪時，地面上的雪最多可以堆積到此處指定的層數。"
  },
  spawnChunkRadius: {
    name: "重生區塊半徑",
    description: "主世界重生點周圍保持載入的區塊數量。"
  },
  spawnMonsters: {
    name: "生成怪物",
    description: "控制怪物能否自然生成。"
  },
  spawnerBlocksEnabled: {
    name: "啟用生怪籠",
    description: ""
  },
  spawnRadius: {
    name: "重生點半徑",
    description: "控制玩家可以生成的重生點周圍區域大小。"
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
    name: "TNT 爆炸中，部分方塊不會跌落戰利品",
    description: "TNT 引起的爆炸中，被破壞方塊的部分跌落物會消失。"
  },
  universalAnger: {
    name: "無差別憤怒",
    description: "被激怒的中立生物會攻擊附近所有玩家，不僅限於激怒它們的玩家。停用「forgive_dead_players」效果更佳。"
  },
  waterSourceConversion: {
    name: "容許流動水轉化為水源",
    description: "流動水的兩面與熔巖源相鄰時轉化為水源。"
  },
};

export default gamerulePresets;
