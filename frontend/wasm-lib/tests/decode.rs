use wasm_lib::decode::{decode, decode_bundle, DecodeError, TILE_BLOCKS};
use wasm_lib::utils::{bitpack, palette_size_to_bits_size};

fn build_otiles(entries: &[(i32, i32, Vec<u8>)]) -> Vec<u8> {
    let mut out = Vec::new();
    out.extend_from_slice(b"OTILES");
    out.extend_from_slice(&(entries.len() as u32).to_be_bytes());
    for (x, z, bytes) in entries {
        let packed: i64 = ((*x as i64) << 32) | ((*z as i64) & 0xFFFF_FFFF);
        out.extend_from_slice(&packed.to_be_bytes());
        out.extend_from_slice(&(bytes.len() as u32).to_be_bytes());
        out.extend_from_slice(bytes);
    }
    out
}

fn build_otile(
    palette: &[&str],
    blocks: &[u16; TILE_BLOCKS],
    heights: &[u16; TILE_BLOCKS],
    biomes_palette: &[&str],
    biomes: &[u16; TILE_BLOCKS],
    height_bits: u32,
) -> Vec<u8> {
    let mut out = Vec::new();
    out.extend_from_slice(b"OTILE");

    // palette part
    out.extend_from_slice(&(palette.len() as u16).to_be_bytes());
    for id in palette {
        let bytes = id.as_bytes();
        out.push(bytes.len() as u8);
        out.extend_from_slice(bytes);
    }

    // block data part — min 4 bits, matches Java `paletteSizeToBitsSize(size, 4)`
    let block_bits = palette_size_to_bits_size(palette.len(), Some(4));
    let packed_blocks = bitpack(blocks, block_bits);
    out.extend_from_slice(&(packed_blocks.len() as u16).to_be_bytes());
    for p in &packed_blocks {
        out.extend_from_slice(&p.to_be_bytes());
    }

    // height map part
    let packed_heights = bitpack(heights, height_bits);
    out.extend_from_slice(&(packed_heights.len() as u16).to_be_bytes());
    for p in &packed_heights {
        out.extend_from_slice(&p.to_be_bytes());
    }

    // biomes palette part
    out.extend_from_slice(&(biomes_palette.len() as u16).to_be_bytes());
    for id in biomes_palette {
        let bytes = id.as_bytes();
        out.push(bytes.len() as u8);
        out.extend_from_slice(bytes);
    }

    // biomes data part — single-biome optimization mirrors Java
    if biomes_palette.len() <= 1 {
        out.extend_from_slice(&1u16.to_be_bytes());
        out.extend_from_slice(&0u64.to_be_bytes());
    } else {
        let biome_bits = palette_size_to_bits_size(biomes_palette.len(), None);
        let packed_biomes = bitpack(biomes, biome_bits);
        out.extend_from_slice(&(packed_biomes.len() as u16).to_be_bytes());
        for p in &packed_biomes {
            out.extend_from_slice(&p.to_be_bytes());
        }
    }

    out
}

fn build_otile_simple(
    palette: &[&str],
    blocks: &[u16; TILE_BLOCKS],
    heights: &[u16; TILE_BLOCKS],
) -> Vec<u8> {
    build_otile(
        palette,
        blocks,
        heights,
        &["minecraft:plains"],
        &[0u16; TILE_BLOCKS],
        9,
    )
}

#[test]
fn round_trip_uniform_tile() {
    // chess pattern of two block types; sloping heights (z * 16 + x).
    let palette = ["minecraft:air", "minecraft:stone"];
    let mut blocks = [0u16; TILE_BLOCKS];
    let mut heights = [0u16; TILE_BLOCKS];
    for z in 0..16 {
        for x in 0..16 {
            let i = z * 16 + x;
            blocks[i] = ((x + z) % 2) as u16;
            heights[i] = (i as u16) * 2;
        }
    }

    let bytes = build_otile_simple(&palette, &blocks, &heights);
    let tile = decode(&bytes).expect("decode ok");

    assert_eq!(tile.palette, vec!["minecraft:air".to_string(), "minecraft:stone".to_string()]);
    assert_eq!(tile.blocks, blocks);
    assert_eq!(tile.heights, heights);
    assert_eq!(tile.biomes_palette, vec!["minecraft:plains".to_string()]);
    assert_eq!(tile.biomes, [0u16; TILE_BLOCKS]);
}

#[test]
fn round_trip_large_palette_uses_more_bits() {
    // 17 entries → bits = max(4, ceil(log2(17))) = max(4, 5) = 5
    assert_eq!(palette_size_to_bits_size(17, Some(4)), 5);
    let palette: Vec<String> = (0..17).map(|i| format!("minecraft:b{i}")).collect();
    let palette_refs: Vec<&str> = palette.iter().map(String::as_str).collect();

    let mut blocks = [0u16; TILE_BLOCKS];
    for i in 0..TILE_BLOCKS {
        blocks[i] = (i % 17) as u16;
    }
    let heights = [42u16; TILE_BLOCKS];

    let bytes = build_otile_simple(&palette_refs, &blocks, &heights);
    let tile = decode(&bytes).expect("decode ok");
    assert_eq!(tile.palette.len(), 17);
    assert_eq!(tile.blocks, blocks);
    assert_eq!(tile.heights, heights);
}

#[test]
fn single_biome_uses_placeholder_long() {
    // biomes_palette.size() <= 1 → writer emits exactly one zero long; every
    // block must decode to biome index 0 regardless of the long's payload.
    let bytes = build_otile_simple(
        &["minecraft:stone"],
        &[0u16; TILE_BLOCKS],
        &[64u16; TILE_BLOCKS],
    );
    let tile = decode(&bytes).expect("decode ok");
    assert_eq!(tile.biomes_palette, vec!["minecraft:plains".to_string()]);
    assert_eq!(tile.biomes, [0u16; TILE_BLOCKS]);
}

#[test]
fn multi_biome_packs_at_native_bits() {
    // 2 biomes → 1 bit/value (no 4-bit floor for biomes). 256 values * 1 bit
    // packs into 4 longs.
    assert_eq!(palette_size_to_bits_size(2, None), 1);

    let biomes_palette = ["minecraft:plains", "minecraft:ocean"];
    let mut biomes = [0u16; TILE_BLOCKS];
    for i in 0..TILE_BLOCKS {
        biomes[i] = (i % 2) as u16;
    }

    let bytes = build_otile(
        &["minecraft:stone"],
        &[0u16; TILE_BLOCKS],
        &[64u16; TILE_BLOCKS],
        &biomes_palette,
        &biomes,
        9,
    );
    let tile = decode(&bytes).expect("decode ok");
    assert_eq!(
        tile.biomes_palette,
        vec!["minecraft:plains".to_string(), "minecraft:ocean".to_string()],
    );
    assert_eq!(tile.biomes, biomes);
}

#[test]
fn bad_magic_errors() {
    let mut bytes = build_otile_simple(
        &["minecraft:air"],
        &[0u16; TILE_BLOCKS],
        &[0u16; TILE_BLOCKS],
    );
    bytes[0] = b'X';
    match decode(&bytes) {
        Err(DecodeError::BadMagic) => {}
        other => panic!("expected BadMagic, got {:?}", other),
    }
}

#[test]
fn ten_bit_height_map_is_inferred_from_long_count() {
    let palette = ["minecraft:stone"];
    let blocks = [0u16; TILE_BLOCKS];
    let mut heights = [0u16; TILE_BLOCKS];
    for (i, height) in heights.iter_mut().enumerate() {
        *height = 320 + (i % 32) as u16;
    }

    let bytes = build_otile(
        &palette,
        &blocks,
        &heights,
        &["minecraft:plains"],
        &[0u16; TILE_BLOCKS],
        10,
    );
    let tile = decode(&bytes).expect("decode ok");
    assert_eq!(tile.heights, heights);
}

#[test]
fn truncated_input_errors() {
    let bytes = build_otile_simple(
        &["minecraft:air"],
        &[0u16; TILE_BLOCKS],
        &[0u16; TILE_BLOCKS],
    );
    // chop off the trailing biome placeholder long
    let truncated = &bytes[..bytes.len() - 8];
    match decode(truncated) {
        Err(DecodeError::Truncated) => {}
        other => panic!("expected Truncated, got {:?}", other),
    }
}

#[test]
fn bundle_empty_decodes_to_zero_entries() {
    let bytes = build_otiles(&[]);
    let entries = decode_bundle(&bytes).expect("decode bundle ok");
    assert!(entries.is_empty());
}

#[test]
fn bundle_round_trip_multiple_tiles() {
    let palette = ["minecraft:air", "minecraft:stone"];

    let mut blocks_a = [0u16; TILE_BLOCKS];
    let mut heights_a = [0u16; TILE_BLOCKS];
    for i in 0..TILE_BLOCKS {
        blocks_a[i] = (i % 2) as u16;
        heights_a[i] = i as u16;
    }
    let tile_a = build_otile_simple(&palette, &blocks_a, &heights_a);

    let blocks_b = [1u16; TILE_BLOCKS];
    let heights_b = [256u16; TILE_BLOCKS];
    let tile_b = build_otile_simple(&palette, &blocks_b, &heights_b);

    let bytes = build_otiles(&[
        (-3, 7, tile_a.clone()),
        (100, -200, tile_b.clone()),
    ]);

    let entries = decode_bundle(&bytes).expect("decode bundle ok");
    assert_eq!(entries.len(), 2);

    assert_eq!(entries[0].x, -3);
    assert_eq!(entries[0].z, 7);
    assert_eq!(entries[0].tile.blocks, blocks_a);
    assert_eq!(entries[0].tile.heights, heights_a);

    assert_eq!(entries[1].x, 100);
    assert_eq!(entries[1].z, -200);
    assert_eq!(entries[1].tile.blocks, blocks_b);
    assert_eq!(entries[1].tile.heights, heights_b);
}

#[test]
fn bundle_bad_magic_errors() {
    let mut bytes = build_otiles(&[]);
    bytes[0] = b'X';
    match decode_bundle(&bytes) {
        Err(DecodeError::BadMagic) => {}
        other => panic!("expected BadMagic, got {:?}", other),
    }
}

#[test]
fn bundle_truncated_errors() {
    let tile = build_otile_simple(
        &["minecraft:air"],
        &[0u16; TILE_BLOCKS],
        &[0u16; TILE_BLOCKS],
    );
    let bytes = build_otiles(&[(0, 0, tile)]);
    // chop off the last byte of the embedded tile payload
    let truncated = &bytes[..bytes.len() - 1];
    match decode_bundle(truncated) {
        Err(DecodeError::Truncated) => {}
        other => panic!("expected Truncated, got {:?}", other),
    }
}

#[test]
fn palette_size_to_bits_size_matches_java() {
    // 2-arg form (min 4 bits) — used for block palettes, mirrors
    // `AnvilUtility.paletteSizeToBitsSize(size, 4)`.
    assert_eq!(palette_size_to_bits_size(0, Some(4)), 4);
    assert_eq!(palette_size_to_bits_size(1, Some(4)), 4);
    assert_eq!(palette_size_to_bits_size(2, Some(4)), 4);
    assert_eq!(palette_size_to_bits_size(16, Some(4)), 4);
    assert_eq!(palette_size_to_bits_size(17, Some(4)), 5);
    assert_eq!(palette_size_to_bits_size(32, Some(4)), 5);
    assert_eq!(palette_size_to_bits_size(33, Some(4)), 6);

    // 1-arg form (no floor) — used for biome palettes, mirrors
    // `AnvilUtility.paletteSizeToBitsSize(size)`.
    assert_eq!(palette_size_to_bits_size(2, None), 1);
    assert_eq!(palette_size_to_bits_size(3, None), 2);
    assert_eq!(palette_size_to_bits_size(4, None), 2);
    assert_eq!(palette_size_to_bits_size(5, None), 3);
    assert_eq!(palette_size_to_bits_size(8, None), 3);
    assert_eq!(palette_size_to_bits_size(9, None), 4);
    assert_eq!(palette_size_to_bits_size(16, None), 4);
    assert_eq!(palette_size_to_bits_size(17, None), 5);
}
