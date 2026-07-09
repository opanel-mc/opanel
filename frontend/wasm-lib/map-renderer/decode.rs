use std::convert::TryInto;

use crate::utils::{bitunpack, palette_size_to_bits_size};

pub const TILE_SIDE: usize = 16;
pub const TILE_BLOCKS: usize = TILE_SIDE * TILE_SIDE;

const MAGIC: [u8; 5] = *b"OTILE";
const BUNDLE_MAGIC: [u8; 6] = *b"OTILES";
const DEFAULT_HEIGHT_BITS: u32 = 9;

#[derive(Debug)]
pub enum DecodeError {
    BadMagic,
    Truncated,
    Utf8(std::str::Utf8Error),
    BadPaletteIndex(u16),
    BadHeightBits(u8),
}

#[derive(Debug)]
pub struct DecodedTile {
    pub palette: Vec<String>,
    pub blocks: [u16; TILE_BLOCKS],
    pub heights: [u16; TILE_BLOCKS],
    pub biomes_palette: Vec<String>,
    pub biomes: [u16; TILE_BLOCKS],
}

#[derive(Debug)]
pub struct DecodedBundleEntry {
    pub x: i32,
    pub z: i32,
    pub tile: DecodedTile,
}

pub fn decode_bundle(bytes: &[u8]) -> Result<Vec<DecodedBundleEntry>, DecodeError> {
    let mut cur = Cursor::new(bytes);

    if cur.read_array::<6>()? != BUNDLE_MAGIC {
        return Err(DecodeError::BadMagic);
    }

    let tile_count = cur.read_u32()? as usize;
    let mut entries = Vec::with_capacity(tile_count);
    for _ in 0..tile_count {
        let packed = cur.read_i64()?;
        let x = (packed >> 32) as i32;
        let z = packed as i32;
        let tile_len = cur.read_u32()? as usize;
        let tile_bytes = cur.read_slice(tile_len)?;
        let tile = decode(tile_bytes)?;
        entries.push(DecodedBundleEntry { x, z, tile });
    }

    Ok(entries)
}

pub fn decode(bytes: &[u8]) -> Result<DecodedTile, DecodeError> {
    let mut cur = Cursor::new(bytes);

    if cur.read_array::<5>()? != MAGIC {
        return Err(DecodeError::BadMagic);
    }

    // decode palette part
    let palette_size = cur.read_u16()? as usize;
    let mut palette = Vec::with_capacity(palette_size);
    for _ in 0..palette_size {
        let len = cur.read_u8()? as usize;
        let raw = cur.read_slice(len)?;
        let s = std::str::from_utf8(raw).map_err(DecodeError::Utf8)?;
        palette.push(s.to_string());
    }

    // decode block data part
    let block_bits = palette_size_to_bits_size(palette_size, Some(4));
    let blocks = read_packed_array::<TILE_BLOCKS>(&mut cur, block_bits)?;
    for &idx in &blocks {
        if (idx as usize) >= palette.len() {
            return Err(DecodeError::BadPaletteIndex(idx));
        }
    }

    // decode height map part
    let heights = read_height_array::<TILE_BLOCKS>(&mut cur)?;

    // decode biomes palette part
    let biomes_palette_size = cur.read_u16()? as usize;
    let mut biomes_palette = Vec::with_capacity(biomes_palette_size);
    for _ in 0..biomes_palette_size {
        let len = cur.read_u8()? as usize;
        let raw = cur.read_slice(len)?;
        let s = std::str::from_utf8(raw).map_err(DecodeError::Utf8)?;
        biomes_palette.push(s.to_string());
    }

    // decode biome data part
    let biomes = if biomes_palette_size <= 1 {
        let long_count = cur.read_u16()? as usize;
        for _ in 0..long_count {
            cur.read_u64()?;
        }
        [0u16; TILE_BLOCKS]
    } else {
        let biome_bits = palette_size_to_bits_size(biomes_palette_size, None);
        read_packed_array::<TILE_BLOCKS>(&mut cur, biome_bits)?
    };
    for &idx in &biomes {
        if (idx as usize) >= biomes_palette.len() {
            return Err(DecodeError::BadPaletteIndex(idx));
        }
    }

    Ok(DecodedTile { palette, blocks, heights, biomes_palette, biomes })
}

fn read_packed_array<const N: usize>(
    cur: &mut Cursor<'_>,
    bits: u32,
) -> Result<[u16; N], DecodeError> {
    let packed = read_packed_longs(cur)?;
    let unpacked = bitunpack(&packed, bits);
    if unpacked.len() < N {
        return Err(DecodeError::Truncated);
    }
    let mut out = [0u16; N];
    out.copy_from_slice(&unpacked[..N]);
    Ok(out)
}

fn read_height_array<const N: usize>(cur: &mut Cursor<'_>) -> Result<[u16; N], DecodeError> {
    // Height data is right after the block longs. New-format tiles store one
    // explicit bits byte here. Legacy tiles have no prefix, so the next byte is
    // the high byte of the u16 long-count, which is 0 for valid legacy tiles;
    // in that case we must not consume it and instead fall back to 9 bits.
    let bits = match cur.peek_u8()? {
        0 => DEFAULT_HEIGHT_BITS,
        raw @ 1..=16 => {
            cur.read_u8()?;
            raw as u32
        }
        raw => return Err(DecodeError::BadHeightBits(raw)),
    };

    let packed = read_packed_longs(cur)?;
    let unpacked = bitunpack(&packed, bits);
    if unpacked.len() < N {
        return Err(DecodeError::Truncated);
    }
    let mut out = [0u16; N];
    out.copy_from_slice(&unpacked[..N]);
    Ok(out)
}

fn read_packed_longs(cur: &mut Cursor<'_>) -> Result<Vec<u64>, DecodeError> {
    let long_count = cur.read_u16()? as usize;
    let mut packed = Vec::with_capacity(long_count);
    for _ in 0..long_count {
        packed.push(cur.read_u64()?);
    }
    Ok(packed)
}

struct Cursor<'a> {
    buf: &'a [u8],
    pos: usize,
}

impl<'a> Cursor<'a> {
    fn new(buf: &'a [u8]) -> Self {
        Self { buf, pos: 0 }
    }

    fn read_array<const N: usize>(&mut self) -> Result<[u8; N], DecodeError> {
        let slice = self.read_slice(N)?;
        Ok(slice.try_into().unwrap())
    }

    fn read_u8(&mut self) -> Result<u8, DecodeError> {
        Ok(self.read_array::<1>()?[0])
    }

    fn peek_u8(&self) -> Result<u8, DecodeError> {
        self.buf
            .get(self.pos)
            .copied()
            .ok_or(DecodeError::Truncated)
    }

    fn read_u16(&mut self) -> Result<u16, DecodeError> {
        Ok(u16::from_be_bytes(self.read_array::<2>()?))
    }

    fn read_u32(&mut self) -> Result<u32, DecodeError> {
        Ok(u32::from_be_bytes(self.read_array::<4>()?))
    }

    fn read_u64(&mut self) -> Result<u64, DecodeError> {
        Ok(u64::from_be_bytes(self.read_array::<8>()?))
    }

    fn read_i64(&mut self) -> Result<i64, DecodeError> {
        Ok(i64::from_be_bytes(self.read_array::<8>()?))
    }

    fn read_slice(&mut self, n: usize) -> Result<&'a [u8], DecodeError> {
        if self.pos.checked_add(n).map_or(true, |end| end > self.buf.len()) {
            return Err(DecodeError::Truncated);
        }
        let slice = &self.buf[self.pos..self.pos + n];
        self.pos += n;
        Ok(slice)
    }
}
