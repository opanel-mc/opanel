use std::io;

use tokio::fs;
use toml_edit::{DocumentMut, TomlError};

pub(crate) const PUMPKIN_CONFIG_PATH: &str = "pumpkin.toml";

pub(crate) async fn read() -> io::Result<Vec<u8>> {
    fs::read(PUMPKIN_CONFIG_PATH).await
}

pub(crate) async fn read_to_string() -> io::Result<String> {
    fs::read_to_string(PUMPKIN_CONFIG_PATH).await
}

pub(crate) fn parse(contents: &str) -> Result<DocumentMut, TomlError> {
    contents.parse()
}

pub(crate) async fn write(contents: impl AsRef<[u8]>) -> io::Result<()> {
    fs::write(PUMPKIN_CONFIG_PATH, contents).await
}
