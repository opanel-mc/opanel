use std::{
    io,
    path::{Component, Path},
};

pub(crate) fn absolute_path_string(path: &Path) -> io::Result<String> {
    std::path::absolute(path).map(|path| path.to_string_lossy().into_owned())
}

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
    use std::path::Path;

    use super::{absolute_path_string, is_safe_file_name};

    #[test]
    fn returns_absolute_path_strings() {
        let path = absolute_path_string(Path::new("world")).unwrap();

        assert!(Path::new(&path).is_absolute());
        assert!(Path::new(&path).ends_with("world"));
    }

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
