use std::path::{Component, Path};

pub(crate) fn is_safe_file_name(file_name: &str) -> bool {
    let path = Path::new(file_name);
    let mut components = path.components();
    !file_name.is_empty()
        && !file_name.contains(['/', '\\', '\0'])
        && matches!(components.next(), Some(Component::Normal(_)))
        && components.next().is_none()
}

#[cfg(test)]
mod tests {
    use super::is_safe_file_name;

    #[test]
    fn accepts_only_single_safe_path_components() {
        for valid in ["world", "My World", "世界-1"] {
            assert!(is_safe_file_name(valid), "{valid:?} should be valid");
        }
        for invalid in ["", ".", "..", "../world", "world/nether", "world\\nether"] {
            assert!(!is_safe_file_name(invalid), "{invalid:?} should be invalid");
        }
    }
}
