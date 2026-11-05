import type { GamerulePreset } from ".";
import { Axe, BedDouble, Bomb, BookMarked, BowArrow, Boxes, BugOff, Cat, Clock, ClockFading, CloudSunRain, Flame, Gauge, GitBranch, HandCoins, Handshake, HeartMinus, HeartPlus, Hourglass, Link, MapPin, Megaphone, Minimize2, MountainSnow, Pickaxe, Radius, Settings, ShieldCheck, Skull, Snowflake, Sprout, Sword, Swords, Trophy, Volume2, Waves, WavesLadder } from "lucide-react";

/** @see https://zh.minecraft.wiki/w/游戏规则#游戏规则列表 */
const gamerulePresets: GamerulePreset[] = [
  {
    id: "advance_time",
    type: "boolean",
    icon: ClockFading
  },
  {
    id: "advance_weather",
    type: "boolean",
    icon: CloudSunRain
  },
  {
    id: "allow_entering_nether_using_portals",
    type: "boolean"
  },
  {
    id: "block_drops",
    type: "boolean",
    icon: Pickaxe
  },
  {
    id: "block_explosion_drop_decay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "command_block_output",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "command_blocks_work",
    type: "boolean",
    icon: Settings
  },
  {
    id: "drowning_damage",
    type: "boolean",
    icon: WavesLadder
  },
  {
    id: "elytra_movement_check",
    type: "boolean"
  },
  {
    id: "ender_pearls_vanish_on_death",
    type: "boolean"
  },
  {
    id: "entity_drops",
    type: "boolean"
  },
  {
    id: "fall_damage",
    type: "boolean",
    icon: HeartMinus
  },
  {
    id: "fire_damage",
    type: "boolean",
    icon: Flame
  },
  {
    id: "fire_spread_radius_around_player",
    type: "number",
    icon: Radius
  },
  {
    id: "forgive_dead_players",
    type: "boolean",
    icon: Handshake
  },
  {
    id: "freeze_damage",
    type: "boolean",
    icon: Snowflake
  },
  {
    id: "global_sound_events",
    type: "boolean",
    icon: Volume2
  },
  {
    id: "immediate_respawn",
    type: "boolean"
  },
  {
    id: "keep_inventory",
    type: "boolean",
    icon: ShieldCheck
  },
  {
    id: "lava_source_conversion",
    type: "boolean",
    icon: Waves
  },
  {
    id: "limited_crafting",
    type: "boolean",
    icon: BookMarked
  },
  {
    id: "locator_bar",
    type: "boolean",
    icon: MapPin
  },
  {
    id: "log_admin_commands",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "max_block_modifications",
    type: "number",
    icon: Boxes
  },
  {
    id: "max_command_forks",
    type: "number",
    icon: GitBranch
  },
  {
    id: "max_command_sequence_length",
    type: "number",
    icon: Link
  },
  {
    id: "max_entity_cramming",
    type: "number",
    icon: Minimize2
  },
  {
    id: "max_minecart_speed",
    type: "number",
    icon: Gauge
  },
  {
    id: "max_snow_accumulation_height",
    type: "number",
    icon: Snowflake
  },
  {
    id: "mob_drops",
    type: "boolean",
    icon: Sword
  },
  {
    id: "mob_explosion_drop_decay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "mob_griefing",
    type: "boolean",
  },
  {
    id: "natural_health_regeneration",
    type: "boolean",
    icon: HeartPlus
  },
  {
    id: "player_movement_check",
    type: "boolean"
  },
  {
    id: "players_nether_portal_creative_delay",
    type: "number",
    icon: Hourglass
  },
  {
    id: "players_nether_portal_default_delay",
    type: "number",
    icon: Hourglass
  },
  {
    id: "players_sleeping_percentage",
    type: "number",
    icon: BedDouble
  },
  {
    id: "projectiles_can_break_blocks",
    type: "boolean",
    icon: BowArrow
  },
  {
    id: "pvp",
    type: "boolean",
    icon: Swords
  },
  {
    id: "raids",
    type: "boolean",
    icon: Axe
  },
  {
    id: "random_tick_speed",
    type: "number",
    icon: Clock
  },
  {
    id: "reduced_debug_info",
    type: "boolean",
    icon: BugOff
  },
  {
    id: "respawn_radius",
    type: "number",
    icon: Radius
  },
  {
    id: "send_command_feedback",
    type: "boolean",
    icon: Megaphone
  },
  {
    id: "show_advancement_messages",
    type: "boolean",
    icon: Trophy
  },
  {
    id: "show_death_messages",
    type: "boolean",
    icon: Skull
  },
  {
    id: "spawn_mobs",
    type: "boolean",
    icon: Cat
  },
  {
    id: "spawn_monsters",
    type: "boolean"
  },
  {
    id: "spawn_patrols",
    type: "boolean",
    icon: Axe
  },
  {
    id: "spawn_phantoms",
    type: "boolean"
  },
  {
    id: "spawn_wandering_traders",
    type: "boolean",
    icon: HandCoins
  },
  {
    id: "spawn_wardens",
    type: "boolean",
    icon: Sword
  },
  {
    id: "spawner_blocks_work",
    type: "boolean"
  },
  {
    id: "spectators_generate_chunks",
    type: "boolean",
    icon: MountainSnow
  },
  {
    id: "spread_vines",
    type: "boolean",
    icon: Sprout
  },
  {
    id: "tnt_explodes",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "tnt_explosion_drop_decay",
    type: "boolean",
    icon: Bomb
  },
  {
    id: "universal_anger",
    type: "boolean",
    icon: Sword
  },
  {
    id: "water_source_conversion",
    type: "boolean",
    icon: Waves
  }
];

export default gamerulePresets;
