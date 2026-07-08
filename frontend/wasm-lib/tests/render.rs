use wasm_lib::decode::{decode, TILE_BLOCKS, TILE_SIDE};
use wasm_lib::render::{render, TILE_RGBA_LEN};
use wasm_lib::utils::{bitpack, palette_size_to_bits_size};

fn build_otile(palette: &[&str], blocks: &[u16; TILE_BLOCKS], heights: &[u16; TILE_BLOCKS]) -> Vec<u8> {
    let mut out = Vec::new();
    out.extend_from_slice(b"OTILE");

    // palette part
    out.extend_from_slice(&(palette.len() as u16).to_be_bytes());
    for id in palette {
        let bytes = id.as_bytes();
        out.push(bytes.len() as u8);
        out.extend_from_slice(bytes);
    }

    // block data part — min 4 bits per index
    let packed_blocks = bitpack(blocks, palette_size_to_bits_size(palette.len(), Some(4)));
    out.extend_from_slice(&(packed_blocks.len() as u16).to_be_bytes());
    for p in &packed_blocks {
        out.extend_from_slice(&p.to_be_bytes());
    }

    // height map part
    let packed_heights = bitpack(heights, 9);
    out.push(9);
    out.extend_from_slice(&(packed_heights.len() as u16).to_be_bytes());
    for p in &packed_heights {
        out.extend_from_slice(&p.to_be_bytes());
    }

    // biomes palette part — single uniform biome
    out.extend_from_slice(&1u16.to_be_bytes());
    let biome = b"minecraft:plains";
    out.push(biome.len() as u8);
    out.extend_from_slice(biome);

    // biomes data part — placeholder single zero long (Java optimization)
    out.extend_from_slice(&1u16.to_be_bytes());
    out.extend_from_slice(&0u64.to_be_bytes());

    out
}

fn pixel(buf: &[u8], x: usize, z: usize) -> [u8; 4] {
    let i = (z * TILE_SIDE + x) * 4;
    [buf[i], buf[i + 1], buf[i + 2], buf[i + 3]]
}

#[test]
fn rendered_buffer_has_expected_size() {
    let bytes = build_otile(
        &["minecraft:stone"],
        &[0u16; TILE_BLOCKS],
        &[64u16; TILE_BLOCKS],
    );
    let tile = decode(&bytes).unwrap();
    let rgba = render(&tile, true, true);
    assert_eq!(rgba.len(), TILE_RGBA_LEN);
    assert_eq!(rgba.len(), 16 * 16 * 4);
}

#[test]
fn air_pixels_are_fully_transparent() {
    let bytes = build_otile(
        &["minecraft:air"],
        &[0u16; TILE_BLOCKS],
        &[0u16; TILE_BLOCKS],
    );
    let tile = decode(&bytes).unwrap();
    let rgba = render(&tile, true, true);
    for chunk in rgba.chunks_exact(4) {
        assert_eq!(chunk, &[0, 0, 0, 0], "air should be fully transparent");
    }
}

#[test]
fn flat_stone_uses_normal_shade() {
    // All stone, all the same height → every pixel should use shade 1
    // (normal) and edge row z=0 also gets shade 1 by convention.
    // Shade 1 for stone from colors.txt: 100 100 100 255.
    let bytes = build_otile(
        &["minecraft:stone"],
        &[0u16; TILE_BLOCKS],
        &[64u16; TILE_BLOCKS],
    );
    let tile = decode(&bytes).unwrap();
    let rgba = render(&tile, true, true);
    for chunk in rgba.chunks_exact(4) {
        assert_eq!(chunk, &[100, 100, 100, 255]);
    }
}
