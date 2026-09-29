use std::{
    io,
    path::{Path, PathBuf},
    sync::Arc,
};

use axum::{
    body::Body,
    extract::{Multipart, Path as AxumPath, State, multipart::MultipartRejection},
    http::{StatusCode, header::CONTENT_TYPE},
    response::{IntoResponse, Response},
};
use tokio::fs;
use tokio_util::io::ReaderStream;
use tracing::error;

use crate::{
    opanel::OPanel,
    utils::upload::read_file,
    web::response::{ApiError, ApiResponse},
};

use super::control::EmptyPayload;

const IMAGE_EXTENSIONS: [&str; 4] = ["png", "jpg", "jpeg", "webp"];
const LOGIN_BANNER: &[u8] =
    include_bytes!("../../../core/src/main/resources/default-login-banner.png");

const KNOWN_ASSETS: &[(&str, &[u8])] = &[("login-banner", LOGIN_BANNER)];

pub(crate) async fn initialize(opanel: &OPanel) {
    for &(name, default_resource) in KNOWN_ASSETS {
        if let Err(error) = load_asset(opanel.storage().root(), name, default_resource).await {
            error!(%error, asset = name, "failed to load panel asset");
        }
    }
}

pub(super) async fn get_asset(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<Response, ApiError> {
    asset_response(opanel.storage().root(), &name).await
}

pub(super) async fn upload_asset(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
    multipart: Result<Multipart, MultipartRejection>,
) -> Result<ApiResponse<EmptyPayload>, ApiError> {
    require_known_asset(&name, "Unknown asset.")?;
    let multipart = multipart.map_err(|error| ApiError::new(error.status(), error.body_text()))?;
    let file = read_file(multipart)
        .await
        .map_err(|error| ApiError::new(error.status(), error.body_text()))?
        .ok_or_else(|| ApiError::new(StatusCode::BAD_REQUEST, "File is missing."))?;
    let extension = image_extension(&file.name)?;

    replace_asset(opanel.storage().root(), &name, extension, &file.bytes)
        .await
        .map_err(asset_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}))
}

pub(super) async fn reset_asset(
    State(opanel): State<Arc<OPanel>>,
    AxumPath(name): AxumPath<String>,
) -> Result<ApiResponse<EmptyPayload>, ApiError> {
    let default_resource = require_known_asset(&name, "Asset not found.")?;
    replace_asset(opanel.storage().root(), &name, "png", default_resource)
        .await
        .map_err(asset_error)?;
    Ok(ApiResponse::ok(EmptyPayload {}))
}

fn require_known_asset(name: &str, message: &'static str) -> Result<&'static [u8], ApiError> {
    KNOWN_ASSETS
        .iter()
        .find_map(|&(known_name, default_resource)| {
            (name == known_name).then_some(default_resource)
        })
        .ok_or_else(|| ApiError::new(StatusCode::NOT_FOUND, message))
}

fn image_extension(file_name: &str) -> Result<&str, ApiError> {
    Path::new(file_name)
        .extension()
        .and_then(|extension| extension.to_str())
        .filter(|extension| IMAGE_EXTENSIONS.contains(extension))
        .ok_or_else(|| {
            ApiError::new(
                StatusCode::BAD_REQUEST,
                "Asset extension name should be one of png, jpg, jpeg and webp.",
            )
        })
}

async fn asset_response(directory: &Path, name: &str) -> Result<Response, ApiError> {
    let default_resource = require_known_asset(name, "Asset not found.")?;
    let path = load_asset(directory, name, default_resource)
        .await
        .map_err(asset_error)?;
    let file = fs::File::open(&path).await.map_err(asset_error)?;
    let content_type = mime_guess::from_path(&path).first_or_octet_stream();
    Ok((
        [(CONTENT_TYPE, content_type.as_ref())],
        Body::from_stream(ReaderStream::new(file)),
    )
        .into_response())
}

async fn load_asset(directory: &Path, name: &str, default_resource: &[u8]) -> io::Result<PathBuf> {
    for extension in IMAGE_EXTENSIONS {
        let path = directory.join(format!("{name}.{extension}"));
        if fs::try_exists(&path).await? {
            return Ok(path);
        }
    }
    let path = directory.join(format!("{name}.png"));
    fs::write(&path, default_resource).await?;
    Ok(path)
}

async fn replace_asset(
    directory: &Path,
    name: &str,
    extension: &str,
    bytes: &[u8],
) -> io::Result<()> {
    // Keep only one variant so uploads and resets remain consistent after a restart.
    for old_extension in IMAGE_EXTENSIONS {
        match fs::remove_file(directory.join(format!("{name}.{old_extension}"))).await {
            Ok(()) => {}
            Err(error) if error.kind() == io::ErrorKind::NotFound => {}
            Err(error) => return Err(error),
        }
    }
    fs::write(directory.join(format!("{name}.{extension}")), bytes).await
}

fn asset_error(error: io::Error) -> ApiError {
    error!(%error, "failed to access panel asset");
    ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
}

#[cfg(test)]
mod tests {
    use axum::body::to_bytes;

    use crate::utils::file::random_temporary_path;

    use super::*;

    async fn temporary_directory() -> PathBuf {
        let path = random_temporary_path(&std::env::temp_dir(), "assets").unwrap();
        fs::create_dir_all(&path).await.unwrap();
        path
    }

    #[tokio::test]
    async fn extracts_and_restores_the_default_assets() {
        let directory = temporary_directory().await;
        for &(name, default_resource) in KNOWN_ASSETS {
            let path = directory.join(format!("{name}.png"));
            for _ in 0..2 {
                let response = asset_response(&directory, name).await.unwrap();
                assert_eq!(response.status(), StatusCode::OK);
                assert_eq!(response.headers()[CONTENT_TYPE], "image/png");
                assert_eq!(
                    to_bytes(response.into_body(), usize::MAX).await.unwrap(),
                    default_resource
                );
                assert_eq!(fs::read(&path).await.unwrap(), default_resource);
                fs::remove_file(&path).await.unwrap();
            }
        }
        fs::remove_dir_all(directory).await.unwrap();
    }

    #[tokio::test]
    async fn loads_custom_images_and_replaces_other_extensions_on_upload_and_reset() {
        let directory = temporary_directory().await;
        for (extension, content_type) in [
            ("png", "image/png"),
            ("jpg", "image/jpeg"),
            ("jpeg", "image/jpeg"),
            ("webp", "image/webp"),
        ] {
            replace_asset(&directory, "login-banner", extension, b"custom image")
                .await
                .unwrap();
            let response = asset_response(&directory, "login-banner").await.unwrap();
            assert_eq!(response.headers()[CONTENT_TYPE], content_type);
            assert_eq!(
                to_bytes(response.into_body(), usize::MAX).await.unwrap(),
                "custom image"
            );
        }
        fs::write(directory.join("login-banner.jpg"), b"stale image")
            .await
            .unwrap();
        replace_asset(&directory, "login-banner", "png", LOGIN_BANNER)
            .await
            .unwrap();
        assert!(!directory.join("login-banner.webp").exists());
        assert!(!directory.join("login-banner.jpg").exists());
        assert_eq!(
            fs::read(
                load_asset(&directory, "login-banner", LOGIN_BANNER)
                    .await
                    .unwrap()
            )
            .await
            .unwrap(),
            LOGIN_BANNER
        );
        fs::remove_dir_all(directory).await.unwrap();
    }

    #[tokio::test]
    async fn rejects_unknown_assets_without_touching_the_filesystem() {
        for name in ["unknown", "../login-banner", "login-banner.png"] {
            let response = asset_response(Path::new("nonexistent-assets-directory"), name)
                .await
                .unwrap_err()
                .into_response();
            assert_eq!(response.status(), StatusCode::NOT_FOUND);
            let body = to_bytes(response.into_body(), usize::MAX).await.unwrap();
            assert_eq!(
                serde_json::from_slice::<serde_json::Value>(&body).unwrap(),
                serde_json::json!({"code": 404, "error": "Asset not found."})
            );
        }
    }

    #[test]
    fn accepts_only_supported_case_sensitive_extensions() {
        for extension in IMAGE_EXTENSIONS {
            assert_eq!(
                image_extension(&format!("banner.{extension}")).unwrap(),
                extension
            );
        }
        for file_name in [
            "banner.gif",
            "banner.PNG",
            "banner",
            ".png",
            "banner.png.exe",
        ] {
            assert_eq!(
                image_extension(file_name)
                    .unwrap_err()
                    .into_response()
                    .status(),
                StatusCode::BAD_REQUEST
            );
        }
    }

    #[tokio::test]
    async fn reports_io_failures_as_api_errors() {
        let directory = temporary_directory().await;
        fs::create_dir(directory.join("login-banner.png"))
            .await
            .unwrap();
        let error = replace_asset(&directory, "login-banner", "webp", b"image")
            .await
            .unwrap_err();
        assert_eq!(
            asset_error(error).into_response().status(),
            StatusCode::INTERNAL_SERVER_ERROR
        );
        fs::remove_dir_all(directory).await.unwrap();
    }
}
