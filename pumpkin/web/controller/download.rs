use std::{
    path::PathBuf,
    pin::Pin,
    sync::Arc,
    task::{Context, Poll},
};

use axum::{
    body::Body,
    extract::{Path, State},
    http::{
        StatusCode,
        header::{CONTENT_DISPOSITION, CONTENT_LENGTH, CONTENT_TYPE},
    },
    response::{IntoResponse, Response},
};
use percent_encoding::{NON_ALPHANUMERIC, utf8_percent_encode};
use tokio::io::{AsyncRead, ReadBuf};
use tokio_util::io::ReaderStream;
use tracing::error;

use crate::{opanel::OPanel, web::response::ApiError};

struct DownloadFile {
    file: tokio::fs::File,
    delete_on_drop: Option<PathBuf>,
}

impl AsyncRead for DownloadFile {
    fn poll_read(
        mut self: Pin<&mut Self>,
        context: &mut Context<'_>,
        buffer: &mut ReadBuf<'_>,
    ) -> Poll<std::io::Result<()>> {
        Pin::new(&mut self.file).poll_read(context, buffer)
    }
}

impl Drop for DownloadFile {
    fn drop(&mut self) {
        if let Some(path) = self.delete_on_drop.take()
            && let Err(error) = std::fs::remove_file(&path)
            && error.kind() != std::io::ErrorKind::NotFound
        {
            error!(%error, path = %path.display(), "failed to remove temporary download");
        }
    }
}

pub(super) async fn download_file(
    State(opanel): State<Arc<OPanel>>,
    Path((id, file_name)): Path<(String, String)>,
) -> Response {
    let Some(entry) = opanel.downloads().take(&id).await else {
        return ApiError::new(StatusCode::NOT_FOUND, "File not found.").into_response();
    };

    let file = match tokio::fs::File::open(&entry.path).await {
        Ok(file) => file,
        Err(error) => {
            if entry.delete_after_download {
                let _ = tokio::fs::remove_file(&entry.path).await;
            }
            error!(%error, path = %entry.path.display(), "failed to open registered download");
            let status = if error.kind() == std::io::ErrorKind::NotFound {
                StatusCode::NOT_FOUND
            } else {
                StatusCode::INTERNAL_SERVER_ERROR
            };
            return ApiError::new(status, error.to_string()).into_response();
        }
    };
    let length = match file.metadata().await {
        Ok(metadata) => metadata.len(),
        Err(error) => {
            if entry.delete_after_download {
                let _ = tokio::fs::remove_file(&entry.path).await;
            }
            error!(%error, path = %entry.path.display(), "failed to inspect registered download");
            return ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string())
                .into_response();
        }
    };
    let encoded_name = utf8_percent_encode(&file_name, NON_ALPHANUMERIC);
    let download = DownloadFile {
        file,
        delete_on_drop: entry.delete_after_download.then_some(entry.path),
    };

    Response::builder()
        .status(StatusCode::OK)
        .header(CONTENT_TYPE, "application/octet-stream")
        .header(CONTENT_LENGTH, length)
        .header(
            CONTENT_DISPOSITION,
            format!("attachment; filename*=UTF-8''{encoded_name}"),
        )
        .body(Body::from_stream(ReaderStream::new(download)))
        .unwrap_or_else(|error| {
            ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, error.to_string()).into_response()
        })
}
