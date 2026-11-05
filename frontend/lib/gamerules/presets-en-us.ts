import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://minecraft.wiki/w/Game_rule?oldid=3876528
 * @see https://minecraft.wiki/w/Game_rule?oldid=3238837
 * @see https://minecraft.wiki/w/Game_rule?oldid=3026514
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 */
export const wikiUrl = "https://minecraft.wiki/w/Game_rule";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "",
    description: "Whether the daylight cycle and moon phases progress."
  },
  advance_weather: {
    name: "",
    description: "Whether the weather can change naturally. The /weather command can still change weather."
  },
  allow_entering_nether_using_portals: {
    name: "",
    description: "Whether the Nether can be entered by using Nether portals."
  },
  block_drops: {
    name: "",
    description: "Whether blocks should have drops."
  },
  block_explosion_drop_decay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a block explosion (e.g. clicking a bed in dimensions other than the Overworld)."
  },
  command_block_output: {
    name: "",
    description: "Whether command blocks should notify admins when they perform commands."
  },
  command_blocks_work: {
    name: "",
    description: "Whether command blocks should be enabled in-game."
  },
  drowning_damage: {
    name: "",
    description: "Whether the player should take damage when drowning."
  },
  elytra_movement_check: {
    name: "",
    description: "Whether the server should check player speed when the player is wearing elytra. Often helps with jittering due to lag in multiplayer."
  },
  ender_pearls_vanish_on_death: {
    name: "",
    description: "Controls whether thrown ender pearls vanish when the player dies."
  },
  entity_drops: {
    name: "",
    description: "Whether entities that are not mobs should have drops."
  },
  fall_damage: {
    name: "",
    description: "Whether the player should take fall damage."
  },
  fire_damage: {
    name: "",
    description: "Whether the player should take damage in fire, lava, campfires, or on magma blocks."
  },
  fire_spread_radius_around_player: {
    name: "",
    description: "Controls the maximum distance in blocks that fire can spread around a player. Set to 0 to disable fire spread. Set to -1 for an unlimited distance."
  },
  forgive_dead_players: {
    name: "",
    description: "Makes angered neutral mobs within 65 x 21 x 65 blocks centered on the targeted player stop being angry when the player dies."
  },
  freeze_damage: {
    name: "",
    description: "Whether the player should take damage when inside powder snow."
  },
  global_sound_events: {
    name: "",
    description: "Whether the wither's spawning sound, the ender dragon's death sound and the end portal activation sound are heard by all players regardless of location."
  },
  immediate_respawn: {
    name: "",
    description: "Players respawn immediately without showing the death screen."
  },
  keep_inventory: {
    name: "",
    description: "Whether the player should keep items and experience in their inventory after death."
  },
  lava_source_conversion: {
    name: "",
    description: "Whether new sources of lava are allowed to form."
  },
  limited_crafting: {
    name: "",
    description: "Whether players can craft only those recipes that they have unlocked."
  },
  locator_bar: {
    name: "",
    description: "Whether the player's locator bar is enabled, indicating the location of players or entities. Can be set to everyone and off in Bedrock Edition."
  },
  log_admin_commands: {
    name: "",
    description: "Whether to log admin commands to server log."
  },
  max_block_modifications: {
    name: "",
    description: "Controls the maximum number of blocks changed when using /clone, /fill, or /fillbiome."
  },
  max_command_forks: {
    name: "",
    description: "The maximum number of forks (contexts) that can be created during one tick. Applies to command blocks and functions."
  },
  max_command_sequence_length: {
    name: "",
    description: "The maximum length of a chain of commands that can be executed during one tick from one execution source. Applies to command blocks and functions."
  },
  max_entity_cramming: {
    name: "",
    description: "The maximum number of pushable entities a mob or player can push, before taking 6HP entity cramming damage per half-second. Setting to 0 disables the rule. Damage affects Survival-mode or Adventure-mode players, and all mobs but bats. Pushable entities include non-Spectator-mode players, any mob except bats, as well as boats and minecarts."
  },
  max_minecart_speed: {
    name: "",
    description: "The maximum speed a minecart may reach."
  },
  max_snow_accumulation_height: {
    name: "",
    description: "The maximum number of snow layers that can be accumulated on each block."
  },
  mob_drops: {
    name: "",
    description: "Whether mobs should drop items and experience orbs."
  },
  mob_explosion_drop_decay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a mob explosion (e.g. Creeper explosion)."
  },
  mob_griefing: {
    name: "",
    description: "Whether creepers, zombies, endermen, ghasts, withers, ender dragons, rabbits, sheep, villagers, silverfish, snow golems, ravagers and end crystals should be able to change blocks, and whether mobs can pick up items. When mob_griefing is disabled, piglins do not pick up gold ingots, but a player can still barter with them by using the item on the mob. Similarly, villagers do not pick up food items but can still breed until they run out of any food already in their inventory. This also affects the capability of zombie-like creatures like zombified piglins and drowned to pathfind to turtle eggs."
  },
  natural_health_regeneration: {
    name: "",
    description: "Whether the player can regenerate health naturally if their hunger is full enough (doesn't affect external healing, such as golden apples, the Regeneration effect, etc.)."
  },
  player_movement_check: {
    name: "",
    description: "Whether the server should check player speed."
  },
  players_nether_portal_creative_delay: {
    name: "",
    description: "Controls the time that a creative player needs to stand in a Nether portal before changing dimensions."
  },
  players_nether_portal_default_delay: {
    name: "",
    description: "Controls the time that a non-creative player needs to stand in a Nether portal before changing dimensions."
  },
  players_sleeping_percentage: {
    name: "",
    description: "What percentage of players in the Overworld must sleep to skip the night. A percentage value of 0 or less allows the night to be skipped by just 1 player, and a percentage value more than 100 prevents players from skipping the night."
  },
  projectiles_can_break_blocks: {
    name: "",
    description: "Whether impact projectiles destroy blocks that are destructible by them, i.e. chorus flowers, pointed dripstone, and decorated pots."
  },
  pvp: {
    name: "",
    description: "Whether players can damage other players."
  },
  raids: {
    name: "",
    description: "Whether raids are enabled."
  },
  random_tick_speed: {
    name: "",
    description: "How often a random tick occurs (such as plant growth, leaf decay, etc.) per chunk section per game tick. 0 and negative values disables random ticks, higher numbers increase random ticks. Setting to a high integer results in high speeds of decay and growth. Numbers over 4096 make plant growth or leaf decay instantaneous."
  },
  reduced_debug_info: {
    name: "",
    description: "Whether the debug screen shows all or reduced information; and whether the effects of F3 + B (entity hitboxes) and F3 + G (chunk boundaries) are shown."
  },
  respawn_radius: {
    name: "",
    description: "The number of blocks outward from the world spawn coordinates that a player spawns in when first joining a server or when dying without a personal spawnpoint. Has no effect on servers where the default game mode is Adventure."
  },
  send_command_feedback: {
    name: "",
    description: "Whether the feedback from commands executed by a player should show up in chat. Also affects the default behavior of whether command blocks store their output text."
  },
  show_advancement_messages: {
    name: "",
    description: "Whether advancements should be announced in chat."
  },
  show_death_messages: {
    name: "",
    description: "Whether death messages are put into chat when a player dies. Also affects whether a message is sent to the pet's owner when the pet dies."
  },
  spawn_mobs: {
    name: "",
    description: "Spawn cycle: spawn_mobs, spawn_monsters (if applicable); Creature spawning during worldgen: spawn_mobs; Zombie reinforcements, endermites: spawn_mobs, spawn_monsters; Patrols: spawn_mobs, spawn_monsters, spawn_patrols; Phantoms: spawn_mobs, spawn_monsters, spawn_phantoms; Wandering traders: spawn_mobs, spawn_wandering_traders; Wardens: spawn_mobs, spawn_monsters, spawn_wardens; Monster spawners and trial spawners: spawn_mobs, spawn_monsters (if applicable), spawner_blocks_work"
  },
  spawn_monsters: {
    name: "",
    description: "Spawn cycle: spawn_mobs, spawn_monsters (if applicable); Zombie reinforcements, endermites: spawn_mobs, spawn_monsters; Patrols: spawn_mobs, spawn_monsters, spawn_patrols; Phantoms: spawn_mobs, spawn_monsters, spawn_phantoms; Wardens: spawn_mobs, spawn_monsters, spawn_wardens; Monster spawners and trial spawners: spawn_mobs, spawn_monsters (if applicable), spawner_blocks_work"
  },
  spawn_patrols: {
    name: "",
    description: "Patrols: spawn_mobs, spawn_monsters, spawn_patrols"
  },
  spawn_phantoms: {
    name: "",
    description: "Phantoms: spawn_mobs, spawn_monsters, spawn_phantoms"
  },
  spawn_wandering_traders: {
    name: "",
    description: "Wandering traders: spawn_mobs, spawn_wandering_traders"
  },
  spawn_wardens: {
    name: "",
    description: "Wardens: spawn_mobs, spawn_monsters, spawn_wardens"
  },
  spawner_blocks_work: {
    name: "",
    description: "Monster spawners and trial spawners: spawn_mobs, spawn_monsters (if applicable), spawner_blocks_work"
  },
  spectators_generate_chunks: {
    name: "",
    description: "Whether players in Spectator mode can generate chunks."
  },
  spread_vines: {
    name: "",
    description: "Whether vines can spread to other blocks. Cave vines, weeping vines, and twisting vines are not affected."
  },
  tnt_explodes: {
    name: "",
    description: "Whether TNT explodes after activation."
  },
  tnt_explosion_drop_decay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a TNT explosion."
  },
  universal_anger: {
    name: "",
    description: "Makes angered neutral mobs attack any nearby player, not just the player that angered them. Works best if forgive_dead_players is disabled."
  },
  water_source_conversion: {
    name: "",
    description: "Whether new sources of water are allowed to form."
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "",
    description: "Whether fire and lava ticks can occur outside of an 8 chunk range of a player."
  },
  allowEnteringNetherUsingPortals: {
    name: "",
    description: "Whether the Nether can be entered by using Nether portals."
  },
  announceAdvancements: {
    name: "",
    description: "Whether advancements should be announced in chat."
  },
  blockExplosionDropDecay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a block explosion (e.g. clicking a bed in dimensions other than the Overworld)."
  },
  commandBlockOutput: {
    name: "",
    description: "Whether command blocks should notify admins when they perform commands."
  },
  commandBlocksEnabled: {
    name: "",
    description: "Whether command blocks should be enabled in-game."
  },
  commandModificationBlockLimit: {
    name: "",
    description: "Controls the maximum number of blocks changed when using /clone, /fill, or /fillbiome."
  },
  disableElytraMovementCheck: {
    name: "",
    description: "Whether the server should skip checking player speed when the player is wearing elytra. Often helps with jittering due to lag in multiplayer."
  },
  disablePlayerMovementCheck: {
    name: "",
    description: "Whether the server should skip checking player speed."
  },
  disableRaids: {
    name: "",
    description: "Whether raids are disabled."
  },
  doDaylightCycle: {
    name: "",
    description: "Whether the daylight cycle and moon phases progress."
  },
  doEntityDrops: {
    name: "",
    description: "Whether entities that are not mobs should have drops."
  },
  doFireTick: {
    name: "",
    description: "Whether fire should spread and naturally extinguish."
  },
  doImmediateRespawn: {
    name: "",
    description: "Players respawn immediately without showing the death screen."
  },
  doInsomnia: {
    name: "",
    description: "Whether phantoms can spawn in the nighttime."
  },
  doLimitedCrafting: {
    name: "",
    description: "Whether players can craft only those recipes that they have unlocked."
  },
  doMobLoot: {
    name: "",
    description: "Whether mobs should drop items and experience orbs."
  },
  doMobSpawning: {
    name: "",
    description: "Whether mobs should spawn naturally, or via global spawning logic, such as for cats, phantoms, patrols, wandering traders, or zombie sieges. Does not affect special spawning attempts, like monster spawners, raids, or iron golems."
  },
  doPatrolSpawning: {
    name: "",
    description: "Whether patrols can spawn."
  },
  doTileDrops: {
    name: "",
    description: "Whether blocks should have drops."
  },
  doTraderSpawning: {
    name: "",
    description: "Whether wandering traders can spawn."
  },
  doVinesSpread: {
    name: "",
    description: "Whether vines can spread to other blocks. Cave vines, weeping vines, and twisting vines are not affected."
  },
  doWardenSpawning: {
    name: "",
    description: "Whether wardens can spawn."
  },
  doWeatherCycle: {
    name: "",
    description: "Whether the weather can change naturally. The /weather command can still change weather."
  },
  drowningDamage: {
    name: "",
    description: "Whether the player should take damage when drowning."
  },
  enderPearlsVanishOnDeath: {
    name: "",
    description: "Controls whether thrown ender pearls vanish when the player dies."
  },
  fallDamage: {
    name: "",
    description: "Whether the player should take fall damage."
  },
  fireDamage: {
    name: "",
    description: "Whether the player should take damage in fire, lava, campfires, or on magma blocks."
  },
  forgiveDeadPlayers: {
    name: "",
    description: "Makes angered neutral mobs within 65 x 21 x 65 blocks centered on the targeted player stop being angry when the player dies."
  },
  freezeDamage: {
    name: "",
    description: "Whether the player should take damage when inside powder snow."
  },
  globalSoundEvents: {
    name: "",
    description: "Whether the wither's spawning sound, the ender dragon's death sound and the end portal activation sound are heard by all players regardless of location."
  },
  keepInventory: {
    name: "",
    description: "Whether the player should keep items and experience in their inventory after death."
  },
  lavaSourceConversion: {
    name: "",
    description: "Whether new sources of lava are allowed to form."
  },
  locatorBar: {
    name: "",
    description: "Whether the player's locator bar is enabled, indicating the location of players or entities."
  },
  logAdminCommands: {
    name: "",
    description: "Whether to log admin commands to server log."
  },
  maxCommandChainLength: {
    name: "",
    description: "The maximum length of a chain of commands that can be executed during one tick. Applies to command blocks and functions."
  },
  maxCommandForkCount: {
    name: "",
    description: "The maximum number of forks (contexts) that can be created during one tick. Applies to command blocks and functions."
  },
  maxEntityCramming: {
    name: "",
    description: "The maximum number of pushable entities a mob or player can push, before taking 6HP entity cramming damage per half-second. Setting to 0 or lower disables the rule. Damage affects Survival-mode or Adventure-mode players, and all mobs but bats. Pushable entities include non-Spectator-mode players, any mob except bats, as well as boats and minecarts."
  },
  minecartMaxSpeed: {
    name: "",
    description: "The maximum speed a minecart may reach."
  },
  mobExplosionDropDecay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a mob explosion (e.g. Creeper explosion)."
  },
  mobGriefing: {
    name: "",
    description: "Whether creepers, zombies, endermen, ghasts, withers, ender dragons, rabbits, sheep, villagers, silverfish, snow golems, ravagers and end crystals should be able to change blocks, and whether mobs can pick up items. When mobGriefing is disabled, piglins do not pick up gold ingots, but a player can still barter with them by using the item on the mob. Similarly, villagers do not pick up food items but can still breed until they run out of any food already in their inventory. This also affects the capability of zombie-like creatures like zombified piglins and drowned to pathfind to turtle eggs."
  },
  naturalRegeneration: {
    name: "",
    description: "Whether the player can regenerate health naturally if their hunger is full enough (doesn't affect external healing, such as golden apples, the Regeneration effect, etc.)."
  },
  playersNetherPortalCreativeDelay: {
    name: "",
    description: "Controls the time that a creative player needs to stand in a nether portal before changing dimensions."
  },
  playersNetherPortalDefaultDelay: {
    name: "",
    description: "Controls the time that a non-creative player needs to stand in a nether portal before changing dimensions."
  },
  playersSleepingPercentage: {
    name: "",
    description: "What percentage of players in the Overworld must sleep to skip the night. A percentage value of 0 or less allows the night to be skipped by just 1 player, and a percentage value more than 100 prevents players from skipping the night."
  },
  projectilesCanBreakBlocks: {
    name: "",
    description: "Whether impact projectiles destroy blocks that are destructible by them, i.e. chorus flowers, pointed dripstone, and decorated pots."
  },
  randomTickSpeed: {
    name: "",
    description: "How often a random block tick occurs (such as plant growth, leaf decay, etc.) per chunk section per game tick. 0 and negative values disables random ticks, higher numbers increase random ticks. Setting to a high integer results in high speeds of decay and growth. Numbers over 4096 make plant growth or leaf decay instantaneous."
  },
  reducedDebugInfo: {
    name: "",
    description: "Whether the debug screen shows all or reduced information; and whether the effects of F3 + B (entity hitboxes) and F3 + G (chunk boundaries) are shown."
  },
  sendCommandFeedback: {
    name: "",
    description: "Whether the feedback from commands executed by a player should show up in chat. Also affects the default behavior of whether command blocks store their output text."
  },
  showDeathMessages: {
    name: "",
    description: "Whether death messages are put into chat when a player dies. Also affects whether a message is sent to the pet's owner when the pet dies."
  },
  snowAccumulationHeight: {
    name: "",
    description: "The maximum number of snow layers that can be accumulated on each block."
  },
  spawnChunkRadius: {
    name: "",
    description: "The size of the spawn chunks of the world."
  },
  spawnMonsters: {
    name: "",
    description: "Whether monsters can spawn naturally."
  },
  spawnerBlocksEnabled: {
    name: "",
    description: "Enable or disable monster spawner blocks."
  },
  spawnRadius: {
    name: "",
    description: "The number of blocks outward from the world spawn coordinates that a player spawns in when first joining a server or when dying without a personal spawnpoint. Has no effect on servers where the default game mode is Adventure."
  },
  spectatorsGenerateChunks: {
    name: "",
    description: "Whether players in Spectator mode can generate chunks."
  },
  tntExplodes: {
    name: "",
    description: "Whether TNT explodes after activation."
  },
  tntExplosionDropDecay: {
    name: "",
    description: "Whether block loot is dropped by all blocks (false) or randomly (true) depending on how far the block is from the center of a TNT explosion."
  },
  universalAnger: {
    name: "",
    description: "Makes angered neutral mobs attack any nearby player, not just the player that angered them. Works best if forgiveDeadPlayers is disabled."
  },
  waterSourceConversion: {
    name: "",
    description: "Whether new sources of water are allowed to form."
  },
};

export default gamerulePresets;
