use std::{
    io,
    path::{Component, Path, PathBuf},
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

pub(crate) fn random_temporary_path(directory: &Path, extension: &str) -> io::Result<PathBuf> {
    let mut random = [0_u8; 16];
    getrandom::fill(&mut random).map_err(io::Error::other)?;
    let name: String = random.iter().map(|byte| format!("{byte:02x}")).collect();
    Ok(directory.join(format!("{name}.{extension}")))
}

#[cfg(test)]
mod tests {
    use std::path::Path;

    use super::{absolute_path_string, is_safe_file_name, random_temporary_path};

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

    #[test]
    fn creates_random_paths_in_the_requested_directory() {
        let directory = Path::new("temporary");
        let first = random_temporary_path(directory, "zip").unwrap();
        let second = random_temporary_path(directory, "zip").unwrap();

        assert_eq!(first.parent(), Some(directory));
        assert_eq!(
            first.extension().and_then(|value| value.to_str()),
            Some("zip")
        );
        assert_ne!(first, second);
    }
}
