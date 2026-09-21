use std::borrow::Cow;

use axum::{
    body::Body,
    extract::Request,
    http::{
        HeaderMap, HeaderValue, Method, StatusCode,
        header::{CACHE_CONTROL, CONTENT_LENGTH, CONTENT_TYPE, ETAG, IF_NONE_MATCH, VARY},
    },
    response::{IntoResponse, Response},
};
use percent_encoding::percent_decode_str;

use opanel_pumpkin_assets as assets;

use super::response::ApiError;

const RSC_CONTENT_TYPE: &str = "text/x-component";
const RSC_HEADER: &str = "Rsc";
const NO_CACHE: &str = "no-cache, no-store, must-revalidate";
const IMMUTABLE_CACHE: &str = "public, max-age=31536000, immutable";

pub async fn serve(request: Request) -> Response {
    if request.method() != Method::GET && request.method() != Method::HEAD {
        return ApiError::method_not_allowed().into_response();
    }

    let asset_path = match resolve_asset_path(request.uri().path()) {
        Ok(path) => path,
        Err(()) => return not_found(request.method()),
    };
    let is_rsc = is_rsc_request(&asset_path, request.headers());

    match assets::get(&asset_path) {
        Some(file) => embedded_response(
            request.method(),
            request.headers(),
            StatusCode::OK,
            &asset_path,
            is_rsc,
            file,
        ),
        None => not_found(request.method()),
    }
}

fn resolve_asset_path(request_path: &str) -> Result<String, ()> {
    if has_invalid_percent_encoding(request_path) {
        return Err(());
    }

    let decoded = percent_decode_str(request_path)
        .decode_utf8()
        .map_err(|_| ())?;
    if decoded.contains('\\') {
        return Err(());
    }

    let mut segments = Vec::new();
    for segment in decoded.trim_start_matches('/').split('/') {
        if segment.is_empty() {
            continue;
        }
        if segment == "." || segment == ".." {
            return Err(());
        }
        segments.push(segment);
    }

    if segments.is_empty() {
        return Ok("index.html".to_string());
    }

    let mut path = segments.join("/");
    let last_segment = segments.last().ok_or(())?;
    if !last_segment.contains('.') {
        path.push_str(".html");
    }
    Ok(path)
}

fn has_invalid_percent_encoding(value: &str) -> bool {
    let bytes = value.as_bytes();
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'%' {
            if index + 2 >= bytes.len()
                || !bytes[index + 1].is_ascii_hexdigit()
                || !bytes[index + 2].is_ascii_hexdigit()
            {
                return true;
            }
            index += 3;
        } else {
            index += 1;
        }
    }
    false
}

fn is_rsc_request(path: &str, headers: &HeaderMap) -> bool {
    path.ends_with(".txt")
        && headers
            .get(RSC_HEADER)
            .is_some_and(|value| value.as_bytes() == b"1")
}

fn not_found(method: &Method) -> Response {
    match assets::get("404.html") {
        Some(file) => embedded_response(
            method,
            &HeaderMap::new(),
            StatusCode::NOT_FOUND,
            "404.html",
            false,
            file,
        ),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

fn embedded_response(
    method: &Method,
    request_headers: &HeaderMap,
    status: StatusCode,
    path: &str,
    is_rsc: bool,
    file: assets::EmbeddedAsset,
) -> Response {
    let etag = format_etag(file.sha256_hash);
    if status == StatusCode::OK && etag_matches(request_headers, &etag) {
        let mut response = StatusCode::NOT_MODIFIED.into_response();
        let headers = response.headers_mut();
        insert_header(headers, ETAG, &etag);
        insert_header(headers, CACHE_CONTROL, cache_control(path));
        add_vary_header(headers, path);
        add_frontend_headers(headers, is_rsc);
        return response;
    }

    let content_length = file.data.len();
    let body = if method == Method::HEAD {
        Body::empty()
    } else {
        match file.data {
            Cow::Borrowed(data) => Body::from(data),
            Cow::Owned(data) => Body::from(data),
        }
    };
    let mut response = Response::new(body);
    *response.status_mut() = status;

    let headers = response.headers_mut();
    let content_type = content_type(path, is_rsc);
    insert_header(headers, CONTENT_TYPE, content_type.as_ref());
    insert_header(headers, CONTENT_LENGTH, &content_length.to_string());
    insert_header(headers, ETAG, &etag);
    insert_header(headers, CACHE_CONTROL, cache_control(path));
    add_vary_header(headers, path);
    add_frontend_headers(headers, is_rsc);
    response
}

fn add_vary_header(headers: &mut HeaderMap, path: &str) {
    if path.ends_with(".txt") {
        headers.append(VARY, HeaderValue::from_static(RSC_HEADER));
    }
}

fn content_type(path: &str, is_rsc: bool) -> Cow<'static, str> {
    if is_rsc {
        return Cow::Borrowed(RSC_CONTENT_TYPE);
    }
    if path.ends_with(".ttf") {
        return Cow::Borrowed("font/ttf");
    }
    if path.ends_with(".otf") {
        return Cow::Borrowed("font/otf");
    }
    if path.ends_with(".woff") {
        return Cow::Borrowed("font/woff");
    }
    if path.ends_with(".woff2") {
        return Cow::Borrowed("font/woff2");
    }

    Cow::Owned(
        mime_guess::from_path(path)
            .first_or_octet_stream()
            .essence_str()
            .to_string(),
    )
}

fn cache_control(path: &str) -> &'static str {
    if path.starts_with("_next/static/") {
        IMMUTABLE_CACHE
    } else {
        NO_CACHE
    }
}

fn format_etag(hash: [u8; 32]) -> String {
    use std::fmt::Write;

    let mut etag = String::with_capacity(66);
    etag.push('"');
    for byte in hash {
        let _ = write!(etag, "{byte:02x}");
    }
    etag.push('"');
    etag
}

fn etag_matches(headers: &HeaderMap, etag: &str) -> bool {
    headers
        .get(IF_NONE_MATCH)
        .and_then(|value| value.to_str().ok())
        .is_some_and(|value| {
            value.split(',').any(|candidate| {
                let candidate = candidate.trim();
                candidate == "*" || candidate == etag || candidate.strip_prefix("W/") == Some(etag)
            })
        })
}

fn add_frontend_headers(headers: &mut HeaderMap, is_rsc: bool) {
    let build_id = assets::build_id();
    if !build_id.is_empty() {
        insert_header(headers, "x-nextjs-deployment-id", build_id);
        if is_rsc {
            insert_header(headers, "X-Vinext-RSC-Compatibility-Id", build_id);
        }
    }
}

fn insert_header(
    headers: &mut HeaderMap,
    name: impl axum::http::header::IntoHeaderName,
    value: &str,
) {
    if let Ok(value) = HeaderValue::from_str(value) {
        headers.insert(name, value);
    }
}

#[cfg(test)]
mod tests {
    use axum::{
        body::to_bytes,
        http::{Method, Request, StatusCode, header},
    };

    use super::{IMMUTABLE_CACHE, NO_CACHE, RSC_HEADER, assets, serve};

    fn rsc_asset_path() -> String {
        assets::iter()
            .find(|path| {
                path.strip_suffix(".txt")
                    .is_some_and(|route| assets::get(&format!("{route}.html")).is_some())
            })
            .expect("frontend build should contain an RSC asset")
            .into_owned()
    }

    #[tokio::test]
    async fn serves_the_embedded_index() {
        let response = serve(Request::get("/").body(axum::body::Body::empty()).unwrap()).await;

        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(response.headers()[header::CONTENT_TYPE], "text/html");
        assert_eq!(
            response.headers()["x-nextjs-deployment-id"],
            assets::build_id()
        );
        assert!(
            !to_bytes(response.into_body(), usize::MAX)
                .await
                .unwrap()
                .is_empty()
        );
    }

    #[tokio::test]
    async fn serves_nested_pages_and_caches_hashed_assets() {
        let nested = serve(
            Request::get("/panel/dashboard")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(nested.status(), StatusCode::OK);
        assert_eq!(nested.headers()[header::CONTENT_TYPE], "text/html");
        assert_eq!(nested.headers()[header::CACHE_CONTROL], NO_CACHE);

        let asset = assets::iter()
            .find(|path| path.starts_with("_next/static/"))
            .expect("frontend build should contain a hashed static asset");
        let asset = format!("/{asset}");
        let response = serve(Request::get(asset).body(axum::body::Body::empty()).unwrap()).await;
        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(response.headers()[header::CACHE_CONTROL], IMMUTABLE_CACHE);
    }

    #[tokio::test]
    async fn serves_rsc_with_compatibility_headers() {
        let rsc = rsc_asset_path();
        let response = serve(
            Request::get(format!("/{rsc}"))
                .header("RSC", "1")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;

        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(response.headers()[header::CONTENT_TYPE], "text/x-component");
        assert_eq!(response.headers()[header::VARY], RSC_HEADER);
        assert_eq!(
            response.headers()["x-vinext-rsc-compatibility-id"],
            assets::build_id()
        );
    }

    #[tokio::test]
    async fn serves_txt_without_rsc_header_as_regular_text() {
        let rsc = rsc_asset_path();
        let response = serve(
            Request::get(format!("/{rsc}"))
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;

        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(response.headers()[header::CONTENT_TYPE], "text/plain");
        assert_eq!(response.headers()[header::VARY], RSC_HEADER);
        assert!(
            !response
                .headers()
                .contains_key("x-vinext-rsc-compatibility-id")
        );
    }

    #[tokio::test]
    async fn varies_not_modified_txt_responses_by_rsc_header() {
        let rsc = rsc_asset_path();
        let response = serve(
            Request::get(format!("/{rsc}"))
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        let etag = response.headers()[header::ETAG].clone();

        let response = serve(
            Request::get(format!("/{rsc}"))
                .header("RSC", "1")
                .header(header::IF_NONE_MATCH, etag)
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;

        assert_eq!(response.status(), StatusCode::NOT_MODIFIED);
        assert_eq!(response.headers()[header::VARY], RSC_HEADER);
        assert_eq!(
            response.headers()["x-vinext-rsc-compatibility-id"],
            assets::build_id()
        );
    }

    #[tokio::test]
    async fn does_not_expose_internal_build_metadata() {
        let response = serve(
            Request::get("/vinext-rsc-compatibility-id")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;

        assert_eq!(response.status(), StatusCode::NOT_FOUND);
    }

    #[tokio::test]
    async fn supports_head_and_etag() {
        let response = serve(
            Request::builder()
                .method(Method::HEAD)
                .uri("/about")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(response.status(), StatusCode::OK);
        let etag = response.headers()[header::ETAG].clone();
        assert!(
            to_bytes(response.into_body(), usize::MAX)
                .await
                .unwrap()
                .is_empty()
        );

        let response = serve(
            Request::get("/about")
                .header(header::IF_NONE_MATCH, etag)
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(response.status(), StatusCode::NOT_MODIFIED);
        assert_eq!(response.headers()[header::CACHE_CONTROL], NO_CACHE);
    }

    #[tokio::test]
    async fn rejects_unsafe_paths_and_methods() {
        let traversal = serve(
            Request::get("/%2e%2e/Cargo.toml")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(traversal.status(), StatusCode::NOT_FOUND);

        let invalid_encoding = serve(
            Request::get("/%ZZ")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(invalid_encoding.status(), StatusCode::NOT_FOUND);

        let rsc_traversal = serve(
            Request::get("/%2e%2e/Cargo.toml.txt")
                .header("RSC", "1")
                .body(axum::body::Body::empty())
                .unwrap(),
        )
        .await;
        assert_eq!(rsc_traversal.status(), StatusCode::NOT_FOUND);

        let post = serve(Request::post("/").body(axum::body::Body::empty()).unwrap()).await;
        assert_eq!(post.status(), StatusCode::METHOD_NOT_ALLOWED);
    }
}
