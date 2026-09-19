pub fn debug(message: &str) {
    tracing::debug!("{message}");
}

pub fn info(message: &str) {
    tracing::info!("{message}");
}

pub fn warn(message: &str) {
    tracing::warn!("{message}");
}

pub fn error(message: &str) {
    tracing::error!("{message}");
}
