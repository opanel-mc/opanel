use pumpkin_plugin_api::logging::{LogLevel::*, log};

pub fn debug(message: &str) {
    log(Debug, message);
}

pub fn info(message: &str) {
    log(Info, message);
}

pub fn warn(message: &str) {
    log(Warn, message);
}

pub fn error(message: &str) {
    log(Error, message);
}
