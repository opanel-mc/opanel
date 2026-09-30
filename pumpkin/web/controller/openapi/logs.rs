use std::sync::Arc;

use axum::{
    extract::{Path, State},
    http::header,
    response::{IntoResponse, Response},
};
use percent_encoding::{NON_ALPHANUMERIC, utf8_percent_encode};

use crate::{opanel::OPanel, web::response::ApiError};

pub(in crate::web::controller) use super::super::logs::{get_log_content, get_log_file_list};

pub(in crate::web::controller) async fn download_log(
    State(_opanel): State<Arc<OPanel>>,
    Path(name): Path<String>,
) -> Result<Response, ApiError> {
    let content = super::super::logs::read_log(name.clone()).await?;
    Ok(download_response(&name, content))
}

fn download_response(name: &str, content: String) -> Response {
    let name = name
        .strip_suffix(".log.gz")
        .map_or_else(|| name.to_string(), |stem| format!("{stem}.log"));
    let encoded_name = utf8_percent_encode(&name, NON_ALPHANUMERIC);
    (
        [
            (header::CONTENT_TYPE, "application/octet-stream".to_string()),
            (
                header::CONTENT_DISPOSITION,
                format!("attachment; filename*=UTF-8''{encoded_name}"),
            ),
        ],
        content,
    )
        .into_response()
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{body::to_bytes, http::StatusCode};

    #[tokio::test]
    async fn download_returns_content_directly_with_a_decoded_archive_filename() {
        let response = download_response("历史.log.gz", "日志\n".into());
        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(
            response.headers()[header::CONTENT_TYPE],
            "application/octet-stream"
        );
        assert_eq!(
            response.headers()[header::CONTENT_DISPOSITION],
            "attachment; filename*=UTF-8''%E5%8E%86%E5%8F%B2%2Elog"
        );
        assert!(!response.headers().contains_key(header::LOCATION));
        assert_eq!(
            to_bytes(response.into_body(), usize::MAX).await.unwrap(),
            "日志\n".as_bytes()
        );
        let response = download_response("latest.log", "live".into());
        assert_eq!(
            response.headers()[header::CONTENT_DISPOSITION],
            "attachment; filename*=UTF-8''latest%2Elog"
        );
    }

    #[tokio::test]
    async fn invalid_names_and_missing_files_keep_the_log_api_errors() {
        for (name, status) in [
            ("../secret.log", StatusCode::BAD_REQUEST),
            ("a\\b.log", StatusCode::BAD_REQUEST),
            ("nonexistent-opanel-test.log", StatusCode::NOT_FOUND),
        ] {
            assert_eq!(
                super::super::super::logs::read_log(name.into())
                    .await
                    .unwrap_err()
                    .into_response()
                    .status(),
                status
            );
        }
    }
}
