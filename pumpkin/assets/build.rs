use std::{
    env, fs,
    path::{Path, PathBuf},
};

const BUILD_HINT: &str = "run `npm --prefix frontend run build` before building OPanel";

fn main() {
    let manifest_dir = PathBuf::from(
        env::var_os("CARGO_MANIFEST_DIR")
            .expect("Cargo must provide CARGO_MANIFEST_DIR to build scripts"),
    );
    let repository_dir = manifest_dir
        .parent()
        .and_then(Path::parent)
        .expect("pumpkin/assets must be located below the OPanel repository root");
    let dist_dir = repository_dir.join("frontend/dist");
    let client_dir = dist_dir.join("client");
    let build_id_path = dist_dir.join("vinext-rsc-compatibility-id");

    println!("cargo::rerun-if-changed={}", client_dir.display());
    println!("cargo::rerun-if-changed={}", build_id_path.display());
    validate_frontend_output(&client_dir, &build_id_path);

    let mut files = Vec::new();
    collect_files(&client_dir, &mut files);
    files.sort();

    if files.is_empty() {
        panic!(
            "frontend build output is empty at {}: {BUILD_HINT}",
            client_dir.display()
        );
    }

    for file in files {
        println!("cargo::rerun-if-changed={}", file.display());
    }
}

fn validate_frontend_output(client_dir: &Path, build_id_path: &Path) {
    if !client_dir.is_dir() {
        panic!(
            "frontend build output was not found at {}: {BUILD_HINT}",
            client_dir.display()
        );
    }

    for required_file in ["index.html", "404.html"] {
        let path = client_dir.join(required_file);
        if !path.is_file() {
            panic!(
                "required frontend build output {} was not found: {BUILD_HINT}",
                path.display()
            );
        }
    }

    let build_id = fs::read_to_string(build_id_path).unwrap_or_else(|error| {
        panic!(
            "failed to read frontend build ID at {}: {error}: {BUILD_HINT}",
            build_id_path.display(),
        )
    });
    if build_id.trim().is_empty() {
        panic!(
            "frontend build ID at {} is empty: {BUILD_HINT}",
            build_id_path.display()
        );
    }
}

fn collect_files(directory: &Path, files: &mut Vec<PathBuf>) {
    let entries = fs::read_dir(directory).unwrap_or_else(|error| {
        panic!(
            "failed to enumerate frontend build output {}: {error}: {BUILD_HINT}",
            directory.display(),
        )
    });

    for entry in entries {
        let entry = entry.unwrap_or_else(|error| {
            panic!(
                "failed to inspect frontend build output {}: {error}: {BUILD_HINT}",
                directory.display(),
            )
        });
        let path = entry.path();
        let file_type = entry.file_type().unwrap_or_else(|error| {
            panic!(
                "failed to inspect frontend build output {}: {error}: {BUILD_HINT}",
                path.display(),
            )
        });

        if file_type.is_dir() {
            collect_files(&path, files);
        } else if file_type.is_file() {
            files.push(path);
        }
    }
}
