use axum::{
    body::Bytes,
    extract::{Multipart, multipart::MultipartError},
};

pub(crate) struct UploadedFile {
    pub(crate) name: String,
    pub(crate) bytes: Bytes,
}

/// Reads the first `file` upload, returning `None` when it is missing or empty.
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

#[cfg(test)]
mod tests {
    use axum::{
        Router,
        body::{Body, to_bytes},
        extract::{DefaultBodyLimit, FromRequest},
        http::{Request, StatusCode},
        routing::post,
    };
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
    async fn reads_large_uploads_when_the_route_disables_the_default_limit() {
        let file_bytes = vec![b'x'; 3 * 1024 * 1024];
        let mut body = b"--test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"banner.png\"\r\n\r\n".to_vec();
        body.extend_from_slice(&file_bytes);
        body.extend_from_slice(b"\r\n--test--\r\n");

        let router = Router::new().route(
            "/upload",
            post(|multipart: Multipart| async {
                read_file(multipart).await.map(|file| file.unwrap().bytes)
            }),
        );
        for (router, expected_status) in [
            (router.clone(), StatusCode::PAYLOAD_TOO_LARGE),
            (router.layer(DefaultBodyLimit::disable()), StatusCode::OK),
        ] {
            let request = Request::post("/upload")
                .header("content-type", "multipart/form-data; boundary=test")
                .body(Body::from(body.clone()))
                .unwrap();
            let response = router.oneshot(request).await.unwrap();
            assert_eq!(response.status(), expected_status);
            if expected_status == StatusCode::OK {
                assert_eq!(
                    to_bytes(response.into_body(), usize::MAX).await.unwrap(),
                    file_bytes
                );
            }
        }
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
