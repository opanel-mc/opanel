use std::{collections::HashMap, sync::Arc};

use axum::{
    extract::{MatchedPath, Request},
    http::{Method, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
};
use axum_extra::extract::cookie::{Cookie, CookieJar, SameSite};
use time::Duration;

use crate::opanel::OPanel;

use super::response::ApiError;

pub(super) const TOKEN_COOKIE_NAME: &str = "token";
const TOKEN_MAX_AGE_SECONDS: i64 = 24 * 60 * 60;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum AuthRouteRole {
    Public,
    PanelSession,
    PanelOrMcp,
}

#[derive(Debug, Clone, Default)]
pub(super) struct AuthRouteRegistry {
    inner: Arc<AuthRouteRegistryInner>,
}

#[derive(Debug, Clone, Default)]
struct AuthRouteRegistryInner {
    roles: HashMap<&'static str, HashMap<Method, AuthRouteRole>>,
}

impl AuthRouteRegistry {
    pub(super) fn register<const METHOD_COUNT: usize, const PATH_COUNT: usize>(
        &mut self,
        methods: [Method; METHOD_COUNT],
        paths: [&'static str; PATH_COUNT],
        role: AuthRouteRole,
    ) {
        let inner = Arc::make_mut(&mut self.inner);
        for path in paths {
            let roles = inner.roles.entry(path).or_default();
            for method in &methods {
                assert!(
                    roles.insert(method.clone(), role).is_none(),
                    "duplicate authorization role for {method} {path}"
                );
            }
        }
    }

    fn role_for(&self, method: &Method, path: &str) -> Option<AuthRouteRole> {
        let method = if method == Method::HEAD {
            &Method::GET
        } else {
            method
        };

        self.inner
            .roles
            .get(path)
            .and_then(|roles| roles.get(method))
            .copied()
    }
}

pub(super) async fn authorize(jar: CookieJar, request: Request, next: Next) -> Response {
    if request.method() == Method::OPTIONS {
        return next.run(request).await;
    }

    let Some(registry) = request.extensions().get::<AuthRouteRegistry>() else {
        tracing::error!("route authorization registry is missing");
        return ApiError::new(
            StatusCode::INTERNAL_SERVER_ERROR,
            "Route authorization is not configured.",
        )
        .into_response();
    };
    let Some(path) = request.extensions().get::<MatchedPath>() else {
        tracing::error!(
            method = %request.method(),
            path = %request.uri().path(),
            "matched route path is missing during authorization"
        );
        return ApiError::new(
            StatusCode::INTERNAL_SERVER_ERROR,
            "Route authorization is not configured.",
        )
        .into_response();
    };
    let Some(role) = registry.role_for(request.method(), path.as_str()) else {
        tracing::error!(
            method = %request.method(),
            path = %path.as_str(),
            "route authorization is not configured"
        );
        return ApiError::new(
            StatusCode::INTERNAL_SERVER_ERROR,
            "Route authorization is not configured.",
        )
        .into_response();
    };

    if role == AuthRouteRole::Public {
        return next.run(request).await;
    }

    // PANEL_OR_MCP intentionally accepts only panel sessions for now. Bearer-token
    // authentication is added together with the MCP authentication implementation.
    let Some(token) = jar.get(TOKEN_COOKIE_NAME).map(Cookie::value) else {
        return ApiError::new(StatusCode::UNAUTHORIZED, "Token is missing.").into_response();
    };

    let Some(opanel) = request.extensions().get::<Arc<OPanel>>() else {
        tracing::error!("OPanel extension is missing from an authenticated route");
        return ApiError::new(
            StatusCode::INTERNAL_SERVER_ERROR,
            "Route authorization is not configured.",
        )
        .into_response();
    };
    let config = opanel.config();
    if !opanel
        .managers()
        .auth()
        .verify_token(token, &config.access_key, &config.salt)
    {
        return (
            remove_token_cookie(jar, config.cookie_secure),
            ApiError::new(StatusCode::UNAUTHORIZED, "Token is invalid."),
        )
            .into_response();
    }

    next.run(request).await
}

pub(super) fn add_token_cookie(jar: CookieJar, token: String, secure: bool) -> CookieJar {
    jar.add(token_cookie(token, secure))
}

pub(super) fn remove_token_cookie(jar: CookieJar, secure: bool) -> CookieJar {
    let mut cookie = token_cookie(String::new(), secure);
    // CookieJar::remove supplies the expiry attributes while preserving the path and
    // security attributes required to address the original cookie.
    cookie.set_max_age(Duration::ZERO);
    jar.remove(cookie)
}

fn token_cookie(token: String, secure: bool) -> Cookie<'static> {
    let mut cookie = Cookie::new(TOKEN_COOKIE_NAME, token);
    cookie.set_http_only(true);
    cookie.set_same_site(SameSite::Lax);
    cookie.set_path("/");
    cookie.set_max_age(Duration::seconds(TOKEN_MAX_AGE_SECONDS));
    cookie.set_secure(secure);
    cookie
}

#[cfg(test)]
mod tests {
    use axum::{
        Extension, Router,
        body::Body,
        http::{HeaderMap, Method, Request, StatusCode, header},
        middleware,
        response::IntoResponse,
        routing::{any, get},
    };
    use axum_extra::extract::cookie::CookieJar;
    use tower::ServiceExt;

    use super::{
        AuthRouteRegistry, AuthRouteRole, TOKEN_COOKIE_NAME, add_token_cookie, authorize,
        remove_token_cookie,
    };

    #[test]
    fn token_cookie_has_the_session_security_attributes() {
        let jar = add_token_cookie(CookieJar::new(), "signed-token".to_string(), true);
        let cookie = jar
            .get(TOKEN_COOKIE_NAME)
            .expect("token cookie should exist");

        assert_eq!(cookie.value(), "signed-token");
        assert_eq!(cookie.http_only(), Some(true));
        assert_eq!(cookie.secure(), Some(true));
        assert_eq!(cookie.path(), Some("/"));
        assert_eq!(
            cookie.max_age().map(|age| age.whole_seconds()),
            Some(86_400)
        );
    }

    #[test]
    fn removing_the_token_cookie_removes_it_from_the_jar() {
        let jar = add_token_cookie(CookieJar::new(), "signed-token".to_string(), false);
        let jar = remove_token_cookie(jar, false);

        assert!(jar.get(TOKEN_COOKIE_NAME).is_none());
    }

    #[test]
    fn token_cookie_response_matches_the_http_contract() {
        let response = (
            add_token_cookie(CookieJar::new(), "signed-token".to_string(), true),
            StatusCode::OK,
        )
            .into_response();
        let set_cookie = response
            .headers()
            .get(header::SET_COOKIE)
            .unwrap()
            .to_str()
            .unwrap();

        assert!(set_cookie.starts_with("token=signed-token"));
        assert!(set_cookie.contains("HttpOnly"));
        assert!(set_cookie.contains("SameSite=Lax"));
        assert!(set_cookie.contains("Path=/"));
        assert!(set_cookie.contains("Max-Age=86400"));
        assert!(set_cookie.contains("Secure"));

        let mut request_headers = HeaderMap::new();
        request_headers.insert(header::COOKIE, "token=invalid".parse().unwrap());
        let response = (
            remove_token_cookie(CookieJar::from_headers(&request_headers), true),
            StatusCode::UNAUTHORIZED,
        )
            .into_response();
        let set_cookie = response
            .headers()
            .get(header::SET_COOKIE)
            .unwrap()
            .to_str()
            .unwrap();
        assert!(set_cookie.starts_with("token="));
        assert!(set_cookie.contains("Max-Age=0"));
        assert!(set_cookie.contains("Path=/"));
    }

    #[tokio::test]
    async fn missing_route_role_is_rejected_but_options_bypasses_auth() {
        let app = Router::new()
            .route("/", any(|| async { StatusCode::NO_CONTENT }))
            .route_layer(middleware::from_fn(authorize))
            .route_layer(Extension(AuthRouteRegistry::default()));

        let response = app
            .clone()
            .oneshot(Request::get("/").body(Body::empty()).unwrap())
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::INTERNAL_SERVER_ERROR);

        let response = app
            .oneshot(
                Request::builder()
                    .method(Method::OPTIONS)
                    .uri("/")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::NO_CONTENT);
    }

    #[tokio::test]
    async fn public_route_role_reaches_the_handler() {
        let mut registry = AuthRouteRegistry::default();
        registry.register([Method::GET], ["/"], AuthRouteRole::Public);
        let app = Router::new()
            .route("/", get(|| async { StatusCode::NO_CONTENT }))
            .route_layer(middleware::from_fn(authorize))
            .route_layer(Extension(registry));

        let response = app
            .oneshot(Request::get("/").body(Body::empty()).unwrap())
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::NO_CONTENT);
    }

    #[tokio::test]
    async fn nested_parameterized_routes_use_the_full_matched_path() {
        let mut registry = AuthRouteRegistry::default();
        registry.register([Method::GET], ["/api/items/{id}"], AuthRouteRole::Public);
        let app = Router::new()
            .nest(
                "/api",
                Router::new().route("/items/{id}", get(|| async { StatusCode::NO_CONTENT })),
            )
            .route_layer(middleware::from_fn(authorize))
            .route_layer(Extension(registry));

        let response = app
            .oneshot(
                Request::get("/api/items/example")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::NO_CONTENT);
    }

    #[tokio::test]
    async fn head_uses_the_get_role() {
        let mut registry = AuthRouteRegistry::default();
        registry.register([Method::GET], ["/icon"], AuthRouteRole::Public);
        let app = Router::new()
            .route("/icon", get(|| async { StatusCode::NO_CONTENT }))
            .route_layer(middleware::from_fn(authorize))
            .route_layer(Extension(registry));

        for method in [Method::GET, Method::HEAD] {
            let response = app
                .clone()
                .oneshot(
                    Request::builder()
                        .method(method)
                        .uri("/icon")
                        .body(Body::empty())
                        .unwrap(),
                )
                .await
                .unwrap();
            assert_eq!(response.status(), StatusCode::NO_CONTENT);
        }
    }

    #[tokio::test]
    async fn unregistered_method_on_a_public_path_is_rejected() {
        let mut registry = AuthRouteRegistry::default();
        registry.register([Method::GET], ["/public"], AuthRouteRole::Public);
        let app = Router::new()
            .route("/public", get(|| async { StatusCode::NO_CONTENT }))
            .route_layer(middleware::from_fn(authorize))
            .route_layer(Extension(registry));

        let response = app
            .oneshot(
                Request::builder()
                    .method(Method::POST)
                    .uri("/public")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::INTERNAL_SERVER_ERROR);
    }
}
