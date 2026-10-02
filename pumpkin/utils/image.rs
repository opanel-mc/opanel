use std::io::Cursor;

use image::{ImageReader, ImageResult, Limits};

/// Reads dimensions without decoding the pixel data; this does not validate the full image.
pub(crate) fn dimensions(bytes: &[u8], limits: Limits) -> ImageResult<(u32, u32)> {
    let mut reader = ImageReader::new(Cursor::new(bytes)).with_guessed_format()?;
    reader.limits(limits);
    reader.into_dimensions()
}

/// Checks that the image can be decoded within the supplied resource limits.
pub(crate) fn validate(bytes: &[u8], limits: Limits) -> ImageResult<()> {
    let mut reader = ImageReader::new(Cursor::new(bytes)).with_guessed_format()?;
    reader.limits(limits);
    reader.decode().map(|_| ())
}
