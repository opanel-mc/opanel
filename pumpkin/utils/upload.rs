use std::{
    io,
    path::{Path, PathBuf},
};

use axum::{
    body::Bytes,
    extract::{
        Multipart,
        multipart::{Field, MultipartError},
    },
};
use thiserror::Error;
use tokio::{fs, io::AsyncWriteExt};
use tracing::error;

use super::file::random_temporary_path;

pub(crate) struct UploadedFile {
    pub(crate) name: String,
    pub(crate) bytes: Bytes,
}

/// Reads the first `file` upload, returning `None` when it is missing or empty.
/// Intended for bounded uploads whose contents are needed in memory, such as icons.
pub(crate) async fn read_file(
    mut multipart: Multipart,
) -> Result<Option<UploadedFile>, MultipartError> {
    while let Some(field) = multipart.next_field().await? {
        if field.name() != Some("file") {
            continue;
        }
        let Some(name) = field.file_name().map(ToOwned::to_owned) else {
            continue;
        };
        let bytes = field.bytes().await?;
        return Ok((!bytes.is_empty()).then_some(UploadedFile { name, bytes }));
    }
    Ok(None)
}

pub(crate) struct TemporaryUpload {
    path: PathBuf,
    pub(crate) size: u64,
}

impl TemporaryUpload {
    pub(crate) fn path(&self) -> &Path {
        &self.path
    }
}

impl Drop for TemporaryUpload {
    fn drop(&mut self) {
        if let Err(error) = std::fs::remove_file(&self.path)
            && error.kind() != io::ErrorKind::NotFound
        {
            error!(%error, path = %self.path.display(), "failed to remove temporary upload");
        }
    }
}

#[derive(Debug, Error)]
pub(crate) enum UploadError {
    #[error(transparent)]
    Multipart(#[from] MultipartError),
    #[error(transparent)]
    Io(#[from] io::Error),
}

/// Spools a field to disk without collecting its contents in memory.
/// The temporary file is removed when the returned upload is dropped, including on errors.
pub(crate) async fn save_field(
    mut field: Field<'_>,
    directory: &Path,
) -> Result<TemporaryUpload, UploadError> {
    fs::create_dir_all(directory).await?;
    let mut upload = TemporaryUpload {
        path: random_temporary_path(directory, "upload")?,
        size: 0,
    };
    let mut output = fs::File::create(&upload.path).await?;
    while let Some(chunk) = field.chunk().await? {
        output.write_all(&chunk).await?;
        upload.size += chunk.len() as u64;
    }
    output.flush().await?;
    Ok(upload)
}

#[cfg(test)]
mod tests {
    use std::convert::Infallible;

    use axum::{
        Router,
        body::{Body, to_bytes},
        extract::{DefaultBodyLimit, FromRequest, State},
        http::{Request, StatusCode},
        routing::post,
    };
    use futures_util::{StreamExt, stream};
    use tokio::io::AsyncReadExt;
    use tower::ServiceExt;

    use super::*;

    async fn multipart(body: &str) -> Multipart {
        let request = Request::post("/")
            .header("content-type", "multipart/form-data; boundary=test")
            .body(Body::from(body.to_owned()))
            .unwrap();
        Multipart::from_request(request, &()).await.unwrap()
    }

    #[tokio::test]
    async fn reads_named_file_and_skips_other_fields() {
        let body = "--test\r\nContent-Disposition: form-data; name=\"other\"\r\n\r\nignored\r\n\
                    --test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"banner.png\"\r\n\r\nimage\r\n--test--\r\n";
        let file = read_file(multipart(body).await).await.unwrap().unwrap();
        assert_eq!(file.name, "banner.png");
        assert_eq!(file.bytes, "image");
    }

    #[tokio::test]
    async fn spools_large_uploads_and_cleans_up_after_success_or_rejection() {
        let directory = random_temporary_path(&std::env::temp_dir(), "upload-test").unwrap();
        fs::create_dir_all(&directory).await.unwrap();
        let router = Router::new()
            .route(
                "/upload",
                post(
                    |State(directory): State<PathBuf>, mut multipart: Multipart| async move {
                        let field = multipart
                            .next_field()
                            .await
                            .map_err(|error| error.status())?
                            .unwrap();
                        let upload =
                            save_field(field, &directory)
                                .await
                                .map_err(|error| match error {
                                    UploadError::Multipart(error) => error.status(),
                                    UploadError::Io(_) => StatusCode::INTERNAL_SERVER_ERROR,
                                })?;
                        // Verify the on-disk content in bounded chunks too.
                        let mut file = fs::File::open(upload.path()).await.unwrap();
                        let mut buffer = [0_u8; 16 * 1024];
                        let mut size = 0_u64;
                        loop {
                            let read = file.read(&mut buffer).await.unwrap();
                            if read == 0 {
                                break;
                            }
                            assert!(buffer[..read].iter().all(|&byte| byte == b'x'));
                            size += read as u64;
                        }
                        assert_eq!(size, upload.size);
                        Ok::<_, StatusCode>(upload.size.to_string())
                    },
                ),
            )
            .with_state(directory.clone());
        for (router, expected_status) in [
            (router.clone(), StatusCode::PAYLOAD_TOO_LARGE),
            (router.layer(DefaultBodyLimit::disable()), StatusCode::OK),
        ] {
            // Generate 12 MiB without constructing one large request buffer.
            let chunks = std::iter::once(Ok::<_, Infallible>(Bytes::from_static(
                b"--test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"banner.png\"\r\n\r\n",
            )))
            .chain(std::iter::repeat_n(Ok(Bytes::from(vec![b'x'; 16 * 1024])), 768))
            .chain(std::iter::once(Ok(Bytes::from_static(b"\r\n--test--\r\n"))));
            let request = Request::post("/upload")
                .header("content-type", "multipart/form-data; boundary=test")
                .body(Body::from_stream(stream::iter(chunks).then(
                    |chunk| async move {
                        tokio::task::yield_now().await;
                        chunk
                    },
                )))
                .unwrap();
            let response = router.oneshot(request).await.unwrap();
            assert_eq!(response.status(), expected_status);
            if expected_status == StatusCode::OK {
                assert_eq!(
                    to_bytes(response.into_body(), usize::MAX).await.unwrap(),
                    (12 * 1024 * 1024).to_string()
                );
            }
            assert!(
                fs::read_dir(&directory)
                    .await
                    .unwrap()
                    .next_entry()
                    .await
                    .unwrap()
                    .is_none()
            );
        }
        fs::remove_dir_all(directory).await.unwrap();
    }

    #[tokio::test]
    async fn treats_missing_empty_and_non_file_fields_as_missing() {
        for body in [
            "--test--\r\n",
            "--test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"empty.png\"\r\n\r\n\r\n--test--\r\n",
            "--test\r\nContent-Disposition: form-data; name=\"file\"\r\n\r\ntext\r\n--test--\r\n",
        ] {
            assert!(read_file(multipart(body).await).await.unwrap().is_none());
        }
    }

    #[tokio::test]
    async fn rejects_truncated_uploads() {
        let body = "--test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"banner.png\"\r\n\r\ntruncated";
        assert!(read_file(multipart(body).await).await.is_err());
    }
}
