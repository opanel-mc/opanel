use base64::{Engine as _, engine::general_purpose::STANDARD};
use sha2::{Digest, Sha256};
use subtle::ConstantTimeEq;
use thiserror::Error;
use tokio::sync::{Semaphore, SemaphorePermit};

pub(crate) const MIN_KEY_LENGTH: usize = 6;
pub(crate) const MAX_NEW_KEY_LENGTH: usize = 256;

const LEGACY_WRAP_PREFIX: &str = "md5-legacy:";
const BCRYPT_COST: u32 = 12;
const MAX_CONCURRENT_KDF: usize = 4;
const MAX_QUEUED_KDF: usize = 8;

static KDF_SEMAPHORE: Semaphore = Semaphore::const_new(MAX_CONCURRENT_KDF);
static REQUEST_SLOT_SEMAPHORE: Semaphore =
    Semaphore::const_new(MAX_CONCURRENT_KDF + MAX_QUEUED_KDF);

#[derive(Debug, Error)]
pub(crate) enum AccessKeyError {
    #[error("invalid access key length")]
    InvalidLength,
    #[error("failed to hash access key: {0}")]
    Bcrypt(#[from] bcrypt::BcryptError),
    #[error("blocking hash worker failed: {0}")]
    Join(#[from] tokio::task::JoinError),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum VerifyResult {
    Invalid,
    Valid,
    ValidNeedsUpgrade,
}

pub(crate) fn try_acquire_request_slot() -> Option<SemaphorePermit<'static>> {
    REQUEST_SLOT_SEMAPHORE.try_acquire().ok()
}

pub(crate) fn is_valid_input_length(key: &str) -> bool {
    !key.is_empty()
}

pub(crate) fn is_valid_new_key_length(key: &str) -> bool {
    let utf16_len = key.encode_utf16().count();
    (MIN_KEY_LENGTH..=MAX_NEW_KEY_LENGTH).contains(&utf16_len)
}

pub(crate) async fn hash(plaintext_key: &str) -> Result<String, AccessKeyError> {
    if !is_valid_input_length(plaintext_key) {
        return Err(AccessKeyError::InvalidLength);
    }

    let prehashed = sha256_base64(plaintext_key);
    bcrypt_hash(prehashed).await
}

pub(crate) async fn wrap_legacy_if_needed(
    stored_key: &str,
) -> Result<Option<String>, AccessKeyError> {
    if !is_raw_md5(stored_key) {
        return Ok(None);
    }

    let wrapped = bcrypt_hash(stored_key.to_owned()).await?;
    Ok(Some(format!("{LEGACY_WRAP_PREFIX}{wrapped}")))
}

pub(crate) async fn verify(plaintext_input: &str, stored_key: &str) -> VerifyResult {
    if !is_valid_input_length(plaintext_input) || stored_key.trim().is_empty() {
        return VerifyResult::Invalid;
    }

    if let Some(wrapped_bcrypt) = stored_key.strip_prefix(LEGACY_WRAP_PREFIX) {
        if !is_bcrypt_hash(wrapped_bcrypt) {
            return VerifyResult::Invalid;
        }

        let legacy_md5 = double_md5_hex(plaintext_input);
        return if bcrypt_verify(legacy_md5, wrapped_bcrypt.to_owned()).await {
            VerifyResult::ValidNeedsUpgrade
        } else {
            VerifyResult::Invalid
        };
    }

    if is_raw_md5(stored_key) {
        let legacy_md5 = double_md5_hex(plaintext_input);
        return if bool::from(legacy_md5.as_bytes().ct_eq(stored_key.as_bytes())) {
            VerifyResult::ValidNeedsUpgrade
        } else {
            VerifyResult::Invalid
        };
    }

    if is_bcrypt_hash(stored_key) {
        let prehashed = sha256_base64(plaintext_input);
        return if bcrypt_verify(prehashed, stored_key.to_owned()).await {
            VerifyResult::Valid
        } else {
            VerifyResult::Invalid
        };
    }

    VerifyResult::Invalid
}

fn is_raw_md5(value: &str) -> bool {
    value.len() == 32
        && value
            .bytes()
            .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
}

fn is_bcrypt_hash(value: &str) -> bool {
    value.starts_with("$2b$") || value.starts_with("$2a$") || value.starts_with("$2y$")
}

fn sha256_base64(input: &str) -> String {
    STANDARD.encode(Sha256::digest(input.as_bytes()))
}

fn double_md5_hex(input: &str) -> String {
    let first = format!("{:x}", md5::compute(input.as_bytes()));
    format!("{:x}", md5::compute(first.as_bytes()))
}

async fn bcrypt_hash(input: String) -> Result<String, AccessKeyError> {
    let permit = KDF_SEMAPHORE
        .acquire()
        .await
        .expect("static KDF semaphore is never closed");
    let parts = tokio::task::spawn_blocking(move || {
        let _permit = permit;
        bcrypt::hash_with_result(input, BCRYPT_COST)
    })
    .await??;
    Ok(parts.format_for_version(bcrypt::Version::TwoB))
}

async fn bcrypt_verify(input: String, stored_hash: String) -> bool {
    let permit = KDF_SEMAPHORE
        .acquire()
        .await
        .expect("static KDF semaphore is never closed");
    tokio::task::spawn_blocking(move || {
        let _permit = permit;
        bcrypt::verify(input, &stored_hash).unwrap_or(false)
    })
    .await
    .unwrap_or(false)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn hashes_and_verifies_modern_access_keys_including_long_inputs() {
        let key_a = format!("{}suffix-a", "a".repeat(300));
        let key_b = format!("{}suffix-b", "a".repeat(300));
        let hashed = hash(&key_a).await.unwrap();

        assert!(hashed.starts_with("$2b$12$"));
        assert_eq!(verify(&key_a, &hashed).await, VerifyResult::Valid);
        assert_eq!(verify(&key_b, &hashed).await, VerifyResult::Invalid);
    }

    #[tokio::test]
    async fn wraps_and_verifies_legacy_double_md5_hashes() {
        let plaintext = "123456";
        let raw_legacy = double_md5_hex(plaintext);
        assert_eq!(raw_legacy, "14e1b600b1fd579f47433b88e8d85291");

        assert_eq!(
            verify(plaintext, &raw_legacy).await,
            VerifyResult::ValidNeedsUpgrade
        );
        assert_eq!(verify("wrong", &raw_legacy).await, VerifyResult::Invalid);

        let wrapped = wrap_legacy_if_needed(&raw_legacy)
            .await
            .unwrap()
            .expect("raw legacy MD5 should be wrapped");
        assert!(wrapped.starts_with("md5-legacy:$2b$12$"));
        assert!(wrap_legacy_if_needed(&wrapped).await.unwrap().is_none());

        assert_eq!(
            verify(plaintext, &wrapped).await,
            VerifyResult::ValidNeedsUpgrade
        );
        assert_eq!(verify("wrong", &wrapped).await, VerifyResult::Invalid);
    }
}
