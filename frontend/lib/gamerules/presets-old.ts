import type { GamerulePreset } from ".";
import { Axe, BedDouble, Bomb, BookMarked, BowArrow, Boxes, BugOff, Cat, Clock, ClockFading, CloudSunRain, Flame, Gauge, GitBranch, HandCoins, Handshake, HeartMinus, HeartPlus, Hourglass, Link, MapPin, Megaphone, Minimize2, MountainSnow, Pickaxe, Radius, Settings, ShieldCheck, Skull, Snowflake, Sprout, Sword, Swords, Trophy, Volume2, Waves, WavesLadder } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/游戏规则#游戏规则列表 */
const gamerulePresetsOld: GamerulePreset[] = [
  {
    id: "allowFireTicksAwayFromPlayer",
    type: "boolean",
    icon: Flame
  },
  {
    id: "allowEnteringNetherUsingPortals",
    type: "boolean"
  },
  {
    id: "announceAdvancements",
    type: "boolean",
    icon: Trophy
  },
  {
    id: "blockExplosionDropDecay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "commandBlockOutput",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "commandBlocksEnabled",
    type: "boolean",
    icon: Settings
  },
  {
    id: "commandModificationBlockLimit",
    type: "number",
    icon: Boxes
  },
  {
    id: "disableElytraMovementCheck",
    type: "boolean"
  },
  {
    id: "disablePlayerMovementCheck",
    type: "boolean"
  },
  {
    id: "disableRaids",
    type: "boolean",
    icon: Axe
  },
  {
    id: "doDaylightCycle",
    type: "boolean",
    icon: ClockFading
  },
  {
    id: "doEntityDrops",
    type: "boolean"
  },
  {
    id: "doFireTick",
    type: "boolean",
    icon: Flame
  },
  {
    id: "doImmediateRespawn",
    type: "boolean",
  },
  {
    id: "doInsomnia",
    type: "boolean",
  },
  {
    id: "doLimitedCrafting",
    type: "boolean",
    icon: BookMarked
  },
  {
    id: "doMobLoot",
    type: "boolean",
    icon: Sword
  },
  {
    id: "doMobSpawning",
    type: "boolean",
    icon: Cat
  },
  {
    id: "doPatrolSpawning",
    type: "boolean",
    icon: Axe
  },
  {
    id: "doTileDrops",
    type: "boolean",
    icon: Pickaxe
  },
  {
    id: "doTraderSpawning",
    type: "boolean",
    icon: HandCoins
  },
  {
    id: "doVinesSpread",
    type: "boolean",
    icon: Sprout
  },
  {
    id: "doWardenSpawning",
    type: "boolean",
    icon: Sword
  },
  {
    id: "doWeatherCycle",
    type: "boolean",
    icon: CloudSunRain
  },
  {
    id: "drowningDamage",
    type: "boolean",
    icon: WavesLadder
  },
  {
    id: "enderPearlsVanishOnDeath",
    type: "boolean"
  },
  {
    id: "fallDamage",
    type: "boolean",
    icon: HeartMinus
  },
  {
    id: "fireDamage",
    type: "boolean",
    icon: Flame
  },
  {
    id: "forgiveDeadPlayers",
    type: "boolean",
    icon: Handshake
  },
  {
    id: "freezeDamage",
    type: "boolean",
    icon: Snowflake
  },
  {
    id: "globalSoundEvents",
    type: "boolean",
    icon: Volume2
  },
  {
    id: "keepInventory",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "lavaSourceConversion",
    type: "boolean",
    icon: Waves
  },
  {
    id: "locatorBar",
    type: "boolean",
    icon: MapPin
  },
  {
    id: "logAdminCommands",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "maxCommandChainLength",
    type: "number",
    icon: Link
  },
  {
    id: "maxCommandForkCount",
    type: "number",
    icon: GitBranch
  },
  {
    id: "maxEntityCramming",
    type: "number",
    icon: Minimize2
  },
  {
    id: "minecartMaxSpeed",
    type: "number",
    icon: Gauge
  },
  {
    id: "mobExplosionDropDecay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "mobGriefing",
    type: "boolean",
  },
  {
    id: "naturalRegeneration",
    type: "boolean",
    icon: HeartPlus
  },
  {
    id: "playersNetherPortalCreativeDelay",
    type: "number",
    icon: Hourglass
  },
  {
    id: "playersNetherPortalDefaultDelay",
    type: "number",
    icon: Hourglass
  },
  {
    id: "playersSleepingPercentage",
    type: "number",
    icon: BedDouble
  },
  {
    id: "projectilesCanBreakBlocks",
    type: "boolean",
    icon: BowArrow
  },
  {
    id: "pvp",
    type: "boolean",
    icon: Swords
  },
  {
    id: "randomTickSpeed",
    type: "number",
    icon: Clock
  },
  {
    id: "reducedDebugInfo",
    type: "boolean",
    icon: BugOff
  },
  {
    id: "sendCommandFeedback",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "showDeathMessages",
    type: "boolean",
    icon: Skull
  },
  {
    id: "snowAccumulationHeight",
    type: "number",
    icon: Snowflake
  },
  {
    id: "spawnChunkRadius",
    type: "number",
    icon: Radius
  },
  {
    id: "spawnMonsters",
    type: "boolean"
  },
  {
    id: "spawnerBlocksEnabled",
    type: "boolean"
  },
  {
    id: "spawnRadius",
    type: "number",
    icon: Radius
  },
  {
    id: "spectatorsGenerateChunks",
    type: "boolean",
    icon: MountainSnow
  },
  {
    id: "tntExplodes",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "tntExplosionDropDecay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "universalAnger",
    type: "boolean",
    icon: Sword
  },
  {
    id: "waterSourceConversion",
    type: "boolean",
    icon: Waves
  }
];

export default gamerulePresetsOld;
