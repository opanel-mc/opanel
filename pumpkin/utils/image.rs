pub(crate) fn dimensions(bytes: &[u8]) -> image::ImageResult<(u32, u32)> {
    let image = image::load_from_memory(bytes)?;
    Ok((image.width(), image.height()))
}
