include!(concat!(env!("OUT_DIR"), "/colors.rs"));

const MAGENTA: [[u8; 4]; 4] = [[0xff, 0x00, 0xff, 0xff]; 4];

/** see https://minecraft.wiki/w/Biome#List_of_biome_climates */
static GRASS_COLORS: phf::Map<&str, [u8; 4]> = phf::phf_map! {
    "minecraft:badlands"                 => [0x90, 0x81, 0x4d, 0xff],
    "minecraft:wooded_badlands"          => [0x90, 0x81, 0x4d, 0xff],
    "minecraft:eroded_badlands"          => [0x90, 0x81, 0x4d, 0xff],
    "minecraft:desert"                   => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:windswept_savanna"        => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:savanna"                  => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:savanna_plateau"          => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:stony_peaks"              => [0x9a, 0xbe, 0x4b, 0xff],
    "minecraft:jungle"                   => [0x59, 0xc9, 0x3c, 0xff],
    "minecraft:bamboo_jungle"            => [0x59, 0xc9, 0x3c, 0xff],
    "minecraft:sparse_jungle"            => [0x64, 0xc7, 0x3f, 0xff],
    "minecraft:mushroom_fields"          => [0x55, 0xc9, 0x3f, 0xff],
    "minecraft:mangrove_swamp"           => [0x6a, 0x70, 0x39, 0xff],
    "minecraft:swamp"                    => [0x6a, 0x70, 0x39, 0xff],
    "minecraft:plains"                   => [0x91, 0xbd, 0x59, 0xff],
    "minecraft:beach"                    => [0x91, 0xbd, 0x59, 0xff],
    "minecraft:sunflower_plains"         => [0x91, 0xbd, 0x59, 0xff],
    "minecraft:deep_dark"                => [0x91, 0xbd, 0x59, 0xff],
    "minecraft:sulfur_caves"             => [0xab, 0xa6, 0x4f, 0xff],
    "minecraft:dripstone_caves"          => [0x91, 0xbd, 0x59, 0xff],
    "minecraft:forest"                   => [0x79, 0xc0, 0x5a, 0xff],
    "minecraft:flower_forest"            => [0x79, 0xc0, 0x5a, 0xff],
    "minecraft:dark_forest"              => [0x50, 0x7a, 0x32, 0xff],
    "minecraft:pale_garden"              => [0x77, 0x82, 0x72, 0xff],
    "minecraft:birch_forest"             => [0x88, 0xbb, 0x67, 0xff],
    "minecraft:old_growth_birch_forest"  => [0x88, 0xbb, 0x67, 0xff],
    "minecraft:dappled_forest"           => [0xdf, 0x68, 0x27, 0xff],
    "minecraft:lush_caves"               => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:the_void"                 => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:river"                    => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:warm_ocean"               => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:lukewarm_ocean"           => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:deep_lukewarm_ocean"      => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:ocean"                    => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:deep_ocean"               => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:cold_ocean"               => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:deep_cold_ocean"          => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:deep_frozen_ocean"        => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:cherry_grove"             => [0xb6, 0xdb, 0x61, 0xff],
    "minecraft:meadow"                   => [0x83, 0xbb, 0x6d, 0xff],
    "minecraft:old_growth_pine_taiga"    => [0x86, 0xb8, 0x7f, 0xff],
    "minecraft:old_growth_spruce_taiga"  => [0x86, 0xb7, 0x83, 0xff],
    "minecraft:taiga"                    => [0x86, 0xb7, 0x83, 0xff],
    "minecraft:windswept_gravelly_hills" => [0x8a, 0xb6, 0x89, 0xff],
    "minecraft:windswept_forest"         => [0x8a, 0xb6, 0x89, 0xff],
    "minecraft:windswept_hills"          => [0x8a, 0xb6, 0x89, 0xff],
    "minecraft:stony_shore"              => [0x8a, 0xb6, 0x89, 0xff],
    "minecraft:snowy_beach"              => [0x83, 0xb5, 0x93, 0xff],
    "minecraft:frozen_river"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:frozen_ocean"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:snowy_plains"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:ice_spikes"               => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:grove"                    => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:snowy_slopes"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:snowy_taiga"              => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:frozen_peaks"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:jagged_peaks"             => [0x80, 0xb4, 0x97, 0xff],
    "minecraft:nether_wastes"            => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:warped_forest"            => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:crimson_forest"           => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:soul_sand_valley"         => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:basalt_deltas"            => [0xbf, 0xb7, 0x55, 0xff],
    "minecraft:the_end"                  => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:end_highlands"            => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:end_midlands"             => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:small_end_islands"        => [0x8e, 0xb9, 0x71, 0xff],
    "minecraft:end_barrens"              => [0x8e, 0xb9, 0x71, 0xff],
}; 

/** see https://minecraft.wiki/w/Biome#List_of_biome_climates */
static WATER_COLORS: phf::Map<&str, [u8; 4]> = phf::phf_map! {
    "minecraft:sulfur_caves"             => [0x34, 0xbf, 0x89, 0xff],
    "minecraft:dappled_forest"           => [0x37, 0x51, 0x54, 0xff],
    "minecraft:badlands"                 => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:wooded_badlands"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:eroded_badlands"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:desert"                   => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:windswept_savanna"        => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:savanna_plateau"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:savanna"                  => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:bamboo_jungle"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:jungle"                   => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:sparse_jungle"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:mushroom_fields"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:beach"                    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:forest"                   => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:flower_forest"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:dark_forest"              => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:birch_forest"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:river"                    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:warm_ocean"               => [0x43, 0xd5, 0xee, 0xff],
    "minecraft:lukewarm_ocean"           => [0x45, 0xad, 0xf2, 0xff],
    "minecraft:deep_lukewarm_ocean"      => [0x45, 0xad, 0xf2, 0xff],
    "minecraft:ocean"                    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:deep_ocean"               => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:cold_ocean"               => [0x3d, 0x57, 0xd6, 0xff],
    "minecraft:deep_cold_ocean"          => [0x3d, 0x57, 0xd6, 0xff],
    "minecraft:frozen_ocean"             => [0x39, 0x38, 0xc9, 0xff],
    "minecraft:deep_frozen_ocean"        => [0x39, 0x38, 0xc9, 0xff],
    "minecraft:frozen_river"             => [0x39, 0x38, 0xc9, 0xff],
    "minecraft:meadow"                   => [0x0e, 0x4e, 0xcf, 0xff],
    "minecraft:cherry_grove"             => [0x5d, 0xb7, 0xef, 0xff],
    "minecraft:old_growth_pine_taiga"    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:taiga"                    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:windswept_forest"         => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:windswept_gravelly_hills" => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:windswept_hills"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:stony_shore"              => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:snowy_plains"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:ice_spikes"               => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:snowy_beach"              => [0x3d, 0x57, 0xd6, 0xff],
    "minecraft:snowy_taiga"              => [0x3d, 0x57, 0xd6, 0xff],
    "minecraft:dripstone_caves"          => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:deep_dark"                => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:lush_caves"               => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:frozen_peaks"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:jagged_peaks"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:stony_peaks"              => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:plains"                   => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:sunflower_plains"         => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:old_growth_birch_forest"  => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:old_growth_spruce_taiga"  => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:grove"                    => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:snowy_slopes"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:pale_garden"              => [0x76, 0x88, 0x9d, 0xff],
    "minecraft:mangrove_swamp"           => [0x3a, 0x7a, 0x6a, 0xff],
    "minecraft:swamp"                    => [0x61, 0x7b, 0x64, 0xff],
    "minecraft:nether_wastes"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:warped_forest"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:crimson_forest"           => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:soul_sand_valley"         => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:basalt_deltas"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:the_end"                  => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:end_highlands"            => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:end_midlands"             => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:small_end_islands"        => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:end_barrens"              => [0x3f, 0x76, 0xe4, 0xff],
    "minecraft:the_void"                 => [0x3f, 0x76, 0xe4, 0xff],
};

/// Look up a Minecraft block id and return its 4 shading levels of RGBA color
/// (brightest → darkest, in that order). Unknown blocks fall back to magenta
/// so missing palette entries are visually obvious.
///
/// If an exact match isn't found, the blockstate suffix (e.g. `[axis=y]`) is
/// stripped and the bare id is tried as a fallback.
pub fn lookup(id: &str) -> [[u8; 4]; 4] {
    if let Some(v) = COLORS.get(id) {
        return *v;
    }
    if let Some(bracket) = id.find('[') {
        if let Some(v) = COLORS.get(&id[..bracket]) {
            return *v;
        }
    }
    MAGENTA
}

pub const DEFAULT_GRASS_COLOR: [u8; 4] = [0x91, 0xbd, 0x59, 0xff];

/// Look up the grass tint RGBA for a Minecraft biome id (e.g. `minecraft:plains`).
/// Unknown biomes fall back to the default plains tint so vegetation still renders.
pub fn lookup_grass(biome: &str) -> [u8; 4] {
    GRASS_COLORS.get(biome).copied().unwrap_or(DEFAULT_GRASS_COLOR)
}

pub const DEFAULT_WATER_COLOR: [u8; 4] = [0x3f, 0x76, 0xe4, 0xff];

/// Look up the Java Edition water color RGBA for a Minecraft biome id.
/// Unknown biomes fall back to the default ocean blue.
pub fn lookup_water(biome: &str) -> [u8; 4] {
    WATER_COLORS.get(biome).copied().unwrap_or(DEFAULT_WATER_COLOR)
}
