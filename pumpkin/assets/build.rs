use std::{
    collections::BTreeMap,
    env, fs,
    path::{Path, PathBuf},
    process::Command,
};

fn main() {
    let manifest_dir = PathBuf::from(
        env::var_os("CARGO_MANIFEST_DIR")
            .expect("Cargo must provide CARGO_MANIFEST_DIR to build scripts"),
    );
    let repository_dir = manifest_dir
        .parent()
        .and_then(Path::parent)
        .expect("pumpkin/assets must be located below the OPanel repository root");
    let frontend_dir = repository_dir.join("frontend");
    let properties_path = repository_dir.join("pumpkin/frontend.properties");
    let out_dir = PathBuf::from(env::var_os("OUT_DIR").expect("Cargo must provide OUT_DIR"));
    let output_dir = out_dir.join("frontend");
    let client_dir = output_dir.join("client");
    let build_id_path = output_dir.join("vinext-rsc-compatibility-id");

    println!("cargo::rerun-if-changed={}", properties_path.display());
    println!("cargo::rerun-if-env-changed=OPANEL_FRONTEND_PREPARED");
    track_frontend_sources(&frontend_dir);
    if env::var("OPANEL_FRONTEND_PREPARED").as_deref() == Ok("1") {
        for resource in ["assets/minecraft", "wasm-lib/pkg"] {
            println!(
                "cargo::rerun-if-changed={}",
                frontend_dir.join(resource).display()
            );
        }
    }

    let properties = fs::read_to_string(&properties_path)
        .unwrap_or_else(|error| panic!("failed to read {}: {error}", properties_path.display()));
    let frontend_env = parse_frontend_env(&properties)
        .unwrap_or_else(|error| panic!("invalid {}: {error}", properties_path.display()));

    for package in ["next", "wasm-pack"] {
        if !frontend_dir
            .join("node_modules")
            .join(package)
            .join("package.json")
            .is_file()
        {
            panic!(
                "frontend dependency {package} is missing; run `npm --prefix frontend ci` before invoking Cargo"
            );
        }
    }

    let mut npm = if cfg!(windows) {
        let mut command = Command::new("cmd.exe");
        command.args(["/d", "/c", "npm.cmd"]);
        command
    } else {
        Command::new("npm")
    };
    let status = npm
        .args(["run", "build"])
        .current_dir(&frontend_dir)
        .envs(frontend_env)
        .env("VITE_OPANEL_VERSION", env!("CARGO_PKG_VERSION"))
        .env("OPANEL_FRONTEND_OUTPUT", &output_dir)
        .env(
            "OPANEL_FRONTEND_PREPARED",
            env::var_os("OPANEL_FRONTEND_PREPARED").unwrap_or_else(|| "0".into()),
        )
        // prelaunch can invoke Cargo through wasm-pack. Keep it off the parent
        // Cargo invocation's target directory and native cross-compilation flags.
        .env("CARGO_TARGET_DIR", frontend_dir.join("wasm-lib/target"))
        .env_remove("CARGO_BUILD_TARGET")
        .env_remove("CARGO_ENCODED_RUSTFLAGS")
        .env_remove("RUSTFLAGS")
        .status()
        .expect("failed to start `npm run build`; install Node.js and npm first");
    if !status.success() {
        panic!("Pumpkin frontend build failed: {status}");
    }
    validate_frontend_output(&client_dir, &build_id_path);

    // rust-embed's compression feature requires a relative, literal folder.
    // Point it at this Cargo output instead of shared frontend/dist files.
    let embed_path = relative_frontend_path(&client_dir, &manifest_dir);
    let declaration = format!(
        "#[derive(Embed)]\n#[folder = {:?}]\n#[compression = \"zstd\"]\nstruct FrontendAssets;\n",
        embed_path
            .to_str()
            .expect("frontend output path must be UTF-8")
    );
    fs::write(out_dir.join("frontend_assets.rs"), declaration)
        .expect("failed to write frontend asset declaration");
}

fn relative_frontend_path(client_dir: &Path, manifest_dir: &Path) -> PathBuf {
    let client_dir = client_dir
        .canonicalize()
        .expect("frontend output must exist");
    let manifest_dir = manifest_dir.canonicalize().expect("asset crate must exist");
    let common = client_dir
        .components()
        .zip(manifest_dir.components())
        .take_while(|(left, right)| left == right)
        .count();
    assert!(
        common > 0,
        "compressed frontend embedding requires CARGO_TARGET_DIR on the same drive as pumpkin/assets"
    );
    let mut relative = PathBuf::new();
    for _ in manifest_dir.components().skip(common) {
        relative.push("..");
    }
    for component in client_dir.components().skip(common) {
        relative.push(component);
    }
    relative
}

fn parse_frontend_env(properties: &str) -> Result<BTreeMap<String, String>, String> {
    let mut variables = BTreeMap::new();
    for (index, line) in properties
        .trim_start_matches('\u{feff}')
        .lines()
        .enumerate()
    {
        let line = line.trim_start();
        if line.is_empty() || line.starts_with('#') || line.starts_with('!') {
            continue;
        }
        let (key, value) = line
            .split_once('=')
            .ok_or_else(|| format!("expected key=value on line {}", index + 1))?;
        let name = key.trim();
        if name.is_empty()
            || !name
                .bytes()
                .all(|byte| byte.is_ascii_alphanumeric() || byte == b'_')
        {
            return Err(format!(
                "invalid frontend variable name on line {}",
                index + 1
            ));
        }
        let name = name.to_ascii_uppercase();
        if variables.insert(name.clone(), value.to_owned()).is_some() {
            return Err(format!("duplicate frontend variable: {name}"));
        }
    }
    if variables
        .get("VITE_OPANEL_TARGET")
        .is_none_or(|target| target.trim().is_empty())
    {
        return Err("VITE_OPANEL_TARGET is required".into());
    }
    Ok(variables)
}

fn track_frontend_sources(frontend_dir: &Path) {
    // Watch source directories recursively, without watching generated outputs
    // that would otherwise invalidate the build script after every invocation.
    for relative_dir in ["", "assets", "wasm-lib"] {
        let directory = frontend_dir.join(relative_dir);
        let entries = fs::read_dir(&directory)
            .unwrap_or_else(|error| panic!("failed to read {}: {error}", directory.display()));
        for entry in entries {
            let entry = entry.expect("failed to inspect frontend input");
            let name = entry.file_name();
            let name = name.to_string_lossy();
            let generated = match relative_dir {
                "" => matches!(
                    name.as_ref(),
                    "node_modules"
                        | "dist"
                        | "build"
                        | "out"
                        | "coverage"
                        | ".next"
                        | ".vinext"
                        | ".vite"
                        | "next-env.d.ts"
                        | "assets"
                        | "wasm-lib"
                ),
                "assets" => name == "minecraft",
                "wasm-lib" => matches!(name.as_ref(), "pkg" | "target"),
                _ => false,
            };
            if !generated && !name.ends_with(".tsbuildinfo") && !name.ends_with(".log") {
                println!("cargo::rerun-if-changed={}", entry.path().display());
            }
        }
    }
}

fn validate_frontend_output(client_dir: &Path, build_id_path: &Path) {
    for required_file in ["index.html", "404.html"] {
        let path = client_dir.join(required_file);
        if !path.is_file() {
            panic!(
                "required frontend build output {} was not found",
                path.display()
            );
        }
    }

    let build_id = fs::read_to_string(build_id_path).unwrap_or_else(|error| {
        panic!(
            "failed to read frontend build ID at {}: {error}",
            build_id_path.display(),
        )
    });
    if build_id.trim().is_empty() {
        panic!("frontend build ID at {} is empty", build_id_path.display());
    }
}

#[cfg(test)]
mod tests {
    use super::parse_frontend_env;

    #[test]
    fn uppercases_property_names_and_preserves_values() {
        let variables = parse_frontend_env(
            "\u{feff}# configuration\r\nVITE_OPANEL_TARGET=pumpkin-26.3\r\n\
             vite_custom=  中文=hello # value  \r\nOther=value\r\n",
        )
        .unwrap();
        assert_eq!(variables.len(), 3);
        assert_eq!(variables["VITE_OPANEL_TARGET"], "pumpkin-26.3");
        assert_eq!(variables["VITE_CUSTOM"], "  中文=hello # value  ");
        assert_eq!(variables["OTHER"], "value");
    }

    #[test]
    fn requires_a_nonempty_target_in_properties() {
        for properties in ["", "VITE_OPANEL_TARGET=", "vite_opanel_target=  "] {
            assert!(parse_frontend_env(properties).is_err());
        }
    }

    #[test]
    fn rejects_invalid_or_conflicting_properties() {
        for invalid in [
            "=value",
            "invalid name=value",
            "no_equals",
            "vite_opanel_target=other",
        ] {
            let properties = format!("VITE_OPANEL_TARGET=pumpkin-26.3\n{invalid}");
            assert!(parse_frontend_env(&properties).is_err());
        }
    }
}
