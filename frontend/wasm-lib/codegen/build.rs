use std::env;
use std::fs::{self, File};
use std::io::{BufReader, BufWriter, Write};
use std::path::{Path, PathBuf};

use serde_json::Value;

#[path = "../map-renderer/utils.rs"]
mod utils;

// Vanilla leaf tints. Grayscale leaf textures are colorized by these constants
// before averaging. Leaves with baked-in color (cherry/azalea/pale_oak) are
// omitted so they fall through to the no-tint path.
const LEAF_TINTS: &[(&str, [u8; 3])] = &[
    ("minecraft:oak_leaves",        [0x59, 0xae, 0x30]),
    ("minecraft:jungle_leaves",     [0x30, 0xbb, 0x0b]),
    ("minecraft:acacia_leaves",     [0xae, 0xa4, 0x2a]),
    ("minecraft:dark_oak_leaves",   [0x59, 0xae, 0x30]),
    ("minecraft:birch_leaves",      [0x80, 0xa7, 0x55]),
    ("minecraft:spruce_leaves",     [0x61, 0x99, 0x61]),
    ("minecraft:mangrove_leaves",   [0x6a, 0x70, 0x39]),
];

// Texture role priority within a single model when picking a representative face.
const TEXTURE_PRIORITY: &[&str] = &["top", "bottom", "side", "inside"];

fn main() {
    println!("cargo:rerun-if-changed=codegen/build.rs");
    println!("cargo:rerun-if-changed=map-renderer/utils.rs");

    let assets_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .expect("wasm-lib parent dir")
        .join("assets/minecraft");
    let blockstates_dir = assets_dir.join("blockstates");
    let models_dir = assets_dir.join("models");
    let textures_dir = assets_dir.join("textures");

    println!("cargo:rerun-if-changed={}", blockstates_dir.display());
    println!("cargo:rerun-if-changed={}", models_dir.display());
    println!("cargo:rerun-if-changed={}", textures_dir.display());

    for dir in [&blockstates_dir, &models_dir, &textures_dir] {
        if !dir.is_dir() {
            panic!("assets dir not found: {}", dir.display());
        }
    }

    let mut entries: Vec<PathBuf> = fs::read_dir(&blockstates_dir)
        .unwrap_or_else(|e| panic!("read blockstates dir {}: {}", blockstates_dir.display(), e))
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| {
            p.is_file()
                && p.extension().and_then(|s| s.to_str()).map(|s| s.eq_ignore_ascii_case("json"))
                    == Some(true)
        })
        .collect();
    entries.sort();

    if entries.is_empty() {
        panic!("blockstates dir is empty: {}", blockstates_dir.display());
    }

    let mut map = phf_codegen::Map::<String>::new();

    for path in &entries {
        println!("cargo:rerun-if-changed={}", path.display());

        let stem = path
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or_else(|| panic!("blockstate filename not utf-8: {}", path.display()));
        let id = format!("minecraft:{}", stem);

        // Water and grass blocks will be colored by biome during rendering,
        // so just skip them here
        if id == "minecraft:water" || id == "minecraft:grass_block" {
            continue;
        }

        let Some(model_ref) = pick_model(path) else { continue; };
        let Some(model_stem) = strip_resource_prefix(&model_ref) else { continue; };
        let model_path = models_dir.join(format!("{}.json", model_stem));
        println!("cargo:rerun-if-changed={}", model_path.display());

        let Some(texture_ref) = pick_texture(&model_path) else { continue; };
        let Some(texture_stem) = strip_resource_prefix(&texture_ref) else { continue; };
        let texture_path = textures_dir.join(format!("{}.png", texture_stem));
        println!("cargo:rerun-if-changed={}", texture_path.display());
        if !texture_path.is_file() {
            continue;
        }

        let tint = LEAF_TINTS.iter().find(|(k, _)| *k == id).map(|(_, c)| *c);
        let base = average_rgba(&texture_path, tint);
        let shades = utils::shade_rgba(base);

        let mut value = String::from("[");
        for (i, s) in shades.iter().enumerate() {
            if i > 0 {
                value.push(',');
            }
            value.push_str(&format!("[{},{},{},{}]", s[0], s[1], s[2], s[3]));
        }
        value.push(']');

        map.entry(id.clone(), &value);
    }

    let out_path = Path::new(&env::var_os("OUT_DIR").unwrap()).join("colors.rs");
    let mut out = BufWriter::new(File::create(&out_path).unwrap());
    writeln!(
        out,
        "pub static COLORS: phf::Map<&'static str, [[u8; 4]; 4]> = {};",
        map.build()
    )
    .unwrap();
}

fn read_json(path: &Path) -> Option<Value> {
    let file = File::open(path).ok()?;
    serde_json::from_reader(BufReader::new(file)).ok()
}

// Pick the model referenced by the first variant (or first multipart entry).
// For a variant or `apply` value that is an array of models, the first entry
// is used.
fn pick_model(blockstate_path: &Path) -> Option<String> {
    let json = read_json(blockstate_path)?;

    if let Some(variants) = json.get("variants").and_then(|v| v.as_object()) {
        let (_, first) = variants.iter().next()?;
        let entry = match first {
            Value::Array(arr) => arr.first()?,
            other => other,
        };
        return entry.get("model").and_then(|v| v.as_str()).map(str::to_string);
    }

    if let Some(parts) = json.get("multipart").and_then(|v| v.as_array()) {
        let first = parts.first()?;
        let apply = first.get("apply")?;
        let entry = match apply {
            Value::Array(arr) => arr.first()?,
            other => other,
        };
        return entry.get("model").and_then(|v| v.as_str()).map(str::to_string);
    }

    None
}

// Pick a texture from a model's `textures` map: priority is top → bottom →
// side → inside, falling back to the first declared texture. In-model `#ref`
// values are resolved one hop.
fn pick_texture(model_path: &Path) -> Option<String> {
    let json = read_json(model_path)?;
    let textures = json.get("textures")?.as_object()?;
    if textures.is_empty() {
        return None;
    }

    let mut value: Option<String> = None;
    for key in TEXTURE_PRIORITY {
        if let Some(s) = textures.get(*key).and_then(texture_value_str) {
            value = Some(s);
            break;
        }
    }
    let value = match value {
        Some(v) => v,
        None => texture_value_str(textures.iter().next()?.1)?,
    };

    if let Some(rest) = value.strip_prefix('#') {
        return textures.get(rest).and_then(texture_value_str);
    }
    Some(value)
}

// A texture entry is either a plain string or an object like
// `{"sprite": "minecraft:block/glass", "force_translucent": true}` (used by
// glass/glass_pane). Extract the underlying resource path in both forms.
fn texture_value_str(value: &Value) -> Option<String> {
    if let Some(s) = value.as_str() {
        return Some(s.to_string());
    }
    value
        .get("sprite")
        .and_then(|v| v.as_str())
        .map(str::to_string)
}

// Strip `minecraft:` namespace and `block/` directory prefix from a resource
// reference, leaving the file stem used by the flattened asset extraction.
fn strip_resource_prefix(s: &str) -> Option<String> {
    let s = s.strip_prefix("minecraft:").unwrap_or(s);
    let s = s.strip_prefix("block/").unwrap_or(s);
    if s.is_empty() { None } else { Some(s.to_string()) }
}

fn tinted(c: u8, t: u8) -> u64 {
    (c as u32 * t as u32 / 255) as u64
}

fn average_rgba(path: &Path, tint: Option<[u8; 3]>) -> [u8; 4] {
    let file = File::open(path).unwrap_or_else(|e| panic!("open {}: {}", path.display(), e));
    let mut decoder = png::Decoder::new(BufReader::new(file));
    decoder.set_transformations(png::Transformations::EXPAND | png::Transformations::STRIP_16);
    let mut reader = decoder
        .read_info()
        .unwrap_or_else(|e| panic!("png read_info {}: {}", path.display(), e));
    let buf_size = reader
        .output_buffer_size()
        .unwrap_or_else(|| panic!("png output_buffer_size unknown for {}", path.display()));
    let mut buf = vec![0u8; buf_size];
    let info = reader
        .next_frame(&mut buf)
        .unwrap_or_else(|e| panic!("png next_frame {}: {}", path.display(), e));
    let pixels = &buf[..info.buffer_size()];

    let (r_sum, g_sum, b_sum, a_sum, count) = match info.color_type {
        png::ColorType::Rgba => {
            let mut r = 0u64;
            let mut g = 0u64;
            let mut b = 0u64;
            let mut a = 0u64;
            let mut c = 0u64;
            for px in pixels.chunks_exact(4) {
                if px[3] == 0 {
                    continue;
                }
                match tint {
                    Some(t) => {
                        r += tinted(px[0], t[0]);
                        g += tinted(px[1], t[1]);
                        b += tinted(px[2], t[2]);
                    }
                    None => {
                        r += px[0] as u64;
                        g += px[1] as u64;
                        b += px[2] as u64;
                    }
                }
                a += px[3] as u64;
                c += 1;
            }
            (r, g, b, a, c)
        }
        png::ColorType::Rgb => {
            let mut r = 0u64;
            let mut g = 0u64;
            let mut b = 0u64;
            let mut c = 0u64;
            for px in pixels.chunks_exact(3) {
                match tint {
                    Some(t) => {
                        r += tinted(px[0], t[0]);
                        g += tinted(px[1], t[1]);
                        b += tinted(px[2], t[2]);
                    }
                    None => {
                        r += px[0] as u64;
                        g += px[1] as u64;
                        b += px[2] as u64;
                    }
                }
                c += 1;
            }
            (r, g, b, 255 * c, c)
        }
        png::ColorType::GrayscaleAlpha => {
            let mut r = 0u64;
            let mut g = 0u64;
            let mut b = 0u64;
            let mut a = 0u64;
            let mut c = 0u64;
            for px in pixels.chunks_exact(2) {
                if px[1] == 0 {
                    continue;
                }
                match tint {
                    Some(t) => {
                        r += tinted(px[0], t[0]);
                        g += tinted(px[0], t[1]);
                        b += tinted(px[0], t[2]);
                    }
                    None => {
                        r += px[0] as u64;
                        g += px[0] as u64;
                        b += px[0] as u64;
                    }
                }
                a += px[1] as u64;
                c += 1;
            }
            (r, g, b, a, c)
        }
        png::ColorType::Grayscale => {
            let mut r = 0u64;
            let mut g = 0u64;
            let mut b = 0u64;
            let mut c = 0u64;
            for &px in pixels {
                match tint {
                    Some(t) => {
                        r += tinted(px, t[0]);
                        g += tinted(px, t[1]);
                        b += tinted(px, t[2]);
                    }
                    None => {
                        r += px as u64;
                        g += px as u64;
                        b += px as u64;
                    }
                }
                c += 1;
            }
            (r, g, b, 255 * c, c)
        }
        other => panic!("{} unsupported color type {:?}", path.display(), other),
    };

    if count == 0 {
        return [0, 0, 0, 0];
    }
    [
        (r_sum / count) as u8,
        (g_sum / count) as u8,
        (b_sum / count) as u8,
        (a_sum / count) as u8,
    ]
}
