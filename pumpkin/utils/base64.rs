use base64::{
    Engine as _, alphabet,
    engine::{DecodePaddingMode, GeneralPurpose, GeneralPurposeConfig},
};

const ENGINE: GeneralPurpose = GeneralPurpose::new(
    &alphabet::STANDARD,
    GeneralPurposeConfig::new().with_decode_padding_mode(DecodePaddingMode::Indifferent),
);

pub(crate) fn decode_string(value: &str) -> Result<String, base64::DecodeError> {
    ENGINE
        .decode(value)
        .map(|bytes| String::from_utf8_lossy(&bytes).into_owned())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn accepts_java_padding_and_decodes_unicode() {
        assert_eq!(decode_string("SGVsbG8=").unwrap(), "Hello");
        assert_eq!(decode_string("SGVsbG8").unwrap(), "Hello");
        assert_eq!(decode_string("5L2g5aW9").unwrap(), "你好");
        assert!(decode_string("not base64!").is_err());
    }
}
