mod controller;
mod endpoint;
mod oidc;
mod response;
mod server;
mod static_files;

pub(crate) use oidc::OidcManager;
pub use server::WebServer;
