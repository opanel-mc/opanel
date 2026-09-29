use std::sync::Arc;

use axum::{
    extract::{Multipart, State, multipart::MultipartRejection},
    http::{StatusCode, header::CONTENT_TYPE},
    response::{IntoResponse, Response},
};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64_ENGINE};
use tokio::fs;
use toml_edit::value;
use tracing::error;

use crate::{
    opanel::OPanel,
    utils::{image, pumpkin_config, upload::read_file},
    web::response::{ApiError, ApiResponse},
};

use super::control::EmptyPayload;

const ICON_PATH: &str = "server-icon.png";
const DATA_URI_PREFIX: &str = "data:image/png;base64,";
// Allow decoder working memory while keeping the budget small for a 64x64 icon.
const ICON_DECODE_MAX_ALLOC: u64 = 8 * 1024 * 1024;

pub(super) async fn get_favicon(State(opanel): State<Arc<OPanel>>) -> Result<Response, ApiError> {
    let context = opanel.context();
    let favicon = context
        .server
        .get_status()
        .lock()
        .unwrap_or_else(std::sync::PoisonError::into_inner)
        .status_response
        .favicon
        .clone();
    favicon_response(favicon.as_deref())
}

pub(super) async fn upload_favicon(
    State(opanel): State<Arc<OPanel>>,
    multipart: Result<Multipart, MultipartRejection>,
) -> Result<ApiResponse<EmptyPayload>, ApiError> {
    let multipart =
        multipart.map_err(|error| ApiError::new(StatusCode::BAD_REQUEST, error.body_text()))?;
    let file = read_file(multipart)
        .await
        .map_err(|error| ApiError::new(error.status(), error.body_text()))?
        .ok_or_else(|| ApiError::new(StatusCode::BAD_REQUEST, "File is missing."))?;
    let bytes = tokio::task::spawn_blocking(move || {
        validate_favicon(&file.name, &file.bytes)?;
        Ok::<_, ApiError>(file.bytes)
    })
    .await
    .map_err(icon_error)??;

    let contents = pumpkin_config::read_to_string().await.map_err(icon_error)?;
    let updated = update_favicon_document(&contents)?;
    fs::write(ICON_PATH, &bytes).await.map_err(icon_error)?;
    pumpkin_config::write(updated).await.map_err(icon_error)?;

    let context = opanel.context();
    context
        .server
        .get_status()
        .lock()
        .unwrap_or_else(std::sync::PoisonError::into_inner)
        .status_response
        .favicon = Some(format!("{DATA_URI_PREFIX}{}", BASE64_ENGINE.encode(&bytes)));

    Ok(ApiResponse::ok(EmptyPayload {}))
}

fn favicon_response(favicon: Option<&str>) -> Result<Response, ApiError> {
    let favicon = favicon.ok_or_else(ApiError::not_found)?;
    let encoded = favicon
        .strip_prefix(DATA_URI_PREFIX)
        .ok_or_else(|| icon_error("Invalid server favicon data URI."))?;
    let bytes = BASE64_ENGINE.decode(encoded).map_err(icon_error)?;
    Ok(([(CONTENT_TYPE, "image/png")], bytes).into_response())
}

fn validate_favicon(file_name: &str, bytes: &[u8]) -> Result<(), ApiError> {
    if !file_name.ends_with(".png") {
        return Err(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Server favicon should be a png.",
        ));
    }
    // Match Java's ImageIO behavior: the suffix is checked separately from decoding.
    let mut limits = ::image::Limits::default();
    limits.max_alloc = Some(ICON_DECODE_MAX_ALLOC);
    let dimensions = image::dimensions(bytes, limits.clone())
        .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "Illegal image bytes"))?;
    if dimensions != (64, 64) {
        return Err(ApiError::new(
            StatusCode::BAD_REQUEST,
            "Server favicon should be 64*64 sized.",
        ));
    }
    // Decode only after checking dimensions, so damaged pixel data is still rejected.
    limits.max_image_width = Some(64);
    limits.max_image_height = Some(64);
    image::validate(bytes, limits)
        .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "Illegal image bytes"))
}

fn update_favicon_document(contents: &str) -> Result<String, ApiError> {
    let mut document = pumpkin_config::parse(contents).map_err(icon_error)?;
    for (key, updated) in [
        ("use_favicon", value(true)),
        ("favicon_path", value(ICON_PATH)),
    ] {
        if let Some(existing) = document.get(key) {
            let decor = existing
                .as_value()
                .ok_or_else(|| icon_error(format!("`{key}` must be a value")))?
                .decor()
                .clone();
            document[key] = updated;
            *document[key].as_value_mut().unwrap().decor_mut() = decor;
        } else {
            document[key] = updated;
        }
    }
    Ok(document.to_string())
}

fn icon_error(error: impl std::fmt::Display) -> ApiError {
    error!(%error, "failed to access server favicon");
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

#[cfg(test)]
mod tests {
    use std::io::Cursor;

    use ::image::{DynamicImage, ImageFormat};
    use axum::body::to_bytes;

    use super::*;

    fn image_bytes(width: u32, height: u32, format: ImageFormat) -> Vec<u8> {
        let mut output = Cursor::new(Vec::new());
        DynamicImage::new_rgb8(width, height)
            .write_to(&mut output, format)
            .unwrap();
        output.into_inner()
    }

    #[test]
    fn validates_filename_and_decoded_dimensions_like_java() {
        let png = image_bytes(64, 64, ImageFormat::Png);
        validate_favicon("icon.png", &png).unwrap();
        for name in ["icon.PNG", "icon.jpg", "icon", "icon.png.exe"] {
            let response = validate_favicon(name, &png).unwrap_err().into_response();
            assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        }
        for (width, height) in [(63, 64), (64, 63), (128, 128)] {
            let bytes = image_bytes(width, height, ImageFormat::Png);
            let response = validate_favicon("icon.png", &bytes)
                .unwrap_err()
                .into_response();
            assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        }
    }

    #[test]
    fn accepts_other_decodable_images_named_png() {
        for format in [
            ImageFormat::Jpeg,
            ImageFormat::Gif,
            ImageFormat::Bmp,
            ImageFormat::Tiff,
        ] {
            validate_favicon("renamed.png", &image_bytes(64, 64, format)).unwrap();
        }
    }

    #[test]
    fn rejects_invalid_or_truncated_images() {
        let png = image_bytes(64, 64, ImageFormat::Png);
        for bytes in [b"not an image".as_slice(), &png[..png.len() / 2]] {
            assert_eq!(
                validate_favicon("icon.png", bytes)
                    .unwrap_err()
                    .into_response()
                    .status(),
                StatusCode::BAD_REQUEST
            );
        }
    }

    #[tokio::test]
    async fn rejects_large_dimensions_before_decoding_pixel_data() {
        // PNG header for a 10000x10000 RGB8 image, ending at the IDAT header.
        // No large pixel buffer is needed to construct or inspect this fixture.
        let bytes = [
            137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 39, 16, 0, 0, 39,
            16, 8, 2, 0, 0, 0, 53, 44, 245, 112, 0, 0, 0, 0, 73, 68, 65, 84,
        ];
        let response = validate_favicon("icon.png", &bytes)
            .unwrap_err()
            .into_response();
        assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        let body = to_bytes(response.into_body(), usize::MAX).await.unwrap();
        assert_eq!(
            serde_json::from_slice::<serde_json::Value>(&body).unwrap()["error"],
            "Server favicon should be 64*64 sized."
        );
    }

    #[tokio::test]
    async fn rejects_damaged_pixel_data_even_when_dimensions_are_valid() {
        let mut bytes = image_bytes(64, 64, ImageFormat::Png);
        let idat = bytes.windows(4).position(|chunk| chunk == b"IDAT").unwrap();
        bytes.truncate(idat + 4);
        assert_eq!(
            image::dimensions(&bytes, ::image::Limits::default()).unwrap(),
            (64, 64)
        );

        let response = validate_favicon("icon.png", &bytes)
            .unwrap_err()
            .into_response();
        assert_eq!(response.status(), StatusCode::BAD_REQUEST);
        let body = to_bytes(response.into_body(), usize::MAX).await.unwrap();
        assert_eq!(
            serde_json::from_slice::<serde_json::Value>(&body).unwrap()["error"],
            "Illegal image bytes"
        );
    }

    #[tokio::test]
    async fn returns_cached_image_bytes_with_png_content_type() {
        let bytes = image_bytes(64, 64, ImageFormat::Png);
        let uri = format!("{DATA_URI_PREFIX}{}", BASE64_ENGINE.encode(&bytes));
        let response = favicon_response(Some(&uri)).unwrap();
        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(response.headers()[CONTENT_TYPE], "image/png");
        assert_eq!(
            to_bytes(response.into_body(), usize::MAX).await.unwrap(),
            bytes
        );
    }

    #[tokio::test]
    async fn missing_and_invalid_cached_icons_use_api_errors() {
        let response = favicon_response(None).unwrap_err().into_response();
        assert_eq!(response.status(), StatusCode::NOT_FOUND);
        let body = to_bytes(response.into_body(), usize::MAX).await.unwrap();
        assert_eq!(
            serde_json::from_slice::<serde_json::Value>(&body).unwrap(),
            serde_json::json!({"code": 404, "error": "Not Found"})
        );
        for uri in ["invalid", "data:image/png;base64,%%%"] {
            assert_eq!(
                favicon_response(Some(uri))
                    .unwrap_err()
                    .into_response()
                    .status(),
                StatusCode::INTERNAL_SERVER_ERROR
            );
        }
    }

    #[test]
    fn persists_icon_settings_and_preserves_other_config_and_comments() {
        let contents = "# server settings\nuse_favicon = false # disabled\nfavicon_path = 'custom.png' # old path\ndefault_level_name = 'world'\n\n[networking.java]\nmotd = 'Hello'\n";
        let updated = update_favicon_document(contents).unwrap();
        let document = pumpkin_config::parse(&updated).unwrap();
        assert_eq!(document["use_favicon"].as_bool(), Some(true));
        assert_eq!(document["favicon_path"].as_str(), Some(ICON_PATH));
        assert_eq!(document["default_level_name"].as_str(), Some("world"));
        assert_eq!(
            document["networking"]["java"]["motd"].as_str(),
            Some("Hello")
        );
        for comment in ["# server settings", "# disabled", "# old path"] {
            assert!(updated.contains(comment));
        }
        assert_eq!(update_favicon_document(&updated).unwrap(), updated);
    }

    #[test]
    fn adds_missing_icon_settings_and_rejects_invalid_config() {
        let document = pumpkin_config::parse(&update_favicon_document("").unwrap()).unwrap();
        assert_eq!(document["use_favicon"].as_bool(), Some(true));
        assert_eq!(document["favicon_path"].as_str(), Some(ICON_PATH));
        for contents in [
            "invalid = [",
            "[use_favicon]\nenabled = true",
            "[favicon_path]\npath = 'icon.png'",
        ] {
            assert!(update_favicon_document(contents).is_err());
        }
    }
}
