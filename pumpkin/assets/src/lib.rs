use std::borrow::Cow;

use rust_embed::Embed;

const BUILD_ID: &str = include_str!("../../../frontend/dist/vinext-rsc-compatibility-id");

#[derive(Embed)]
#[folder = "../../frontend/dist/client/"]
struct FrontendAssets;

pub struct EmbeddedAsset {
    pub data: Cow<'static, [u8]>,
    pub sha256_hash: [u8; 32],
}

pub fn get(path: &str) -> Option<EmbeddedAsset> {
    FrontendAssets::get(path).map(|file| EmbeddedAsset {
        data: file.data,
        sha256_hash: file.metadata.sha256_hash(),
    })
}

pub fn iter() -> impl Iterator<Item = Cow<'static, str>> {
    FrontendAssets::iter()
}

pub fn build_id() -> &'static str {
    BUILD_ID.trim()
}

#[cfg(test)]
mod tests {
    use super::{build_id, get, iter};

    #[test]
    fn embeds_required_frontend_files() {
        assert!(get("index.html").is_some());
        assert!(get("404.html").is_some());
        assert!(!build_id().is_empty());
        assert!(iter().any(|path| path.starts_with("_next/static/")));
    }
}
