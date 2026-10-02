use std::fmt::Write as _;

use pumpkin::command::{snbt::SnbtParser, string_reader::StringReader};
use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};

pub(crate) fn parse_compound(input: &str) -> Option<NbtCompound> {
    let mut reader = StringReader::new(input);
    let NbtTag::Compound(compound) = SnbtParser::parse_for_commands(&mut reader).ok()? else {
        return None;
    };
    reader.skip_whitespace();
    (!reader.can_read_char()).then_some(compound)
}

/// Pumpkin's Display implementation does not escape strings or namespaced keys.
/// Sort keys recursively as well, so equivalent components have a stable inventory hash.
pub(crate) fn compound_to_string(compound: &NbtCompound) -> String {
    let mut output = String::new();
    write_compound(&mut output, compound);
    output
}

fn write_compound(output: &mut String, compound: &NbtCompound) {
    output.push('{');
    let mut entries: Vec<_> = compound.child_tags.iter().collect();
    entries.sort_unstable_by_key(|(key, _)| *key);
    for (index, (key, value)) in entries.into_iter().enumerate() {
        if index > 0 {
            output.push(',');
        }
        output.push_str(&serde_json::to_string(key).expect("strings are serializable"));
        output.push(':');
        write_tag(output, value);
    }
    output.push('}');
}

fn write_tag(output: &mut String, tag: &NbtTag) {
    match tag {
        NbtTag::Compound(compound) => write_compound(output, compound),
        NbtTag::String(value) => {
            output.push_str(&serde_json::to_string(value).expect("strings are serializable"));
        }
        NbtTag::List(items) => {
            output.push('[');
            for (index, item) in items.iter().enumerate() {
                if index > 0 {
                    output.push(',');
                }
                write_tag(output, item);
            }
            output.push(']');
        }
        _ => write!(output, "{tag}").expect("writing to a String cannot fail"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trips_quoted_keys_strings_and_numeric_types() {
        let mut data = parse_compound(
            r#"{"minecraft:custom_data":{b:1b,s:2s,i:3,l:4L,f:1.5f,d:2.5d,bytes:[B;-1b,2b],ints:[I;1,2],longs:[L;3L,4L],list:[{a:1},{b:2}]}}"#,
        ).unwrap();
        data.put_string("escaped:\"key", "多行\n\"text\" \\ path\t\0 🐈".into());
        assert_eq!(parse_compound(&compound_to_string(&data)), Some(data));
    }

    #[test]
    fn canonicalizes_nested_key_order_and_rejects_non_compounds_or_trailing_input() {
        let first = parse_compound("{b:{d:1,c:2},a:3}").unwrap();
        let second = parse_compound("{a:3,b:{c:2,d:1}}").unwrap();
        assert_eq!(compound_to_string(&first), compound_to_string(&second));
        for invalid in ["[]", "1", "{", "{} garbage", "{}{}"] {
            assert!(parse_compound(invalid).is_none(), "{invalid}");
        }
        assert!(parse_compound("  {}  ").is_some());
    }
}
