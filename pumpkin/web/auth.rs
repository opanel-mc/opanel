use std::{
    collections::HashMap,
    sync::{Mutex, MutexGuard, PoisonError},
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
};

use base64::{Engine as _, engine::general_purpose::URL_SAFE_NO_PAD};
use hmac::{Hmac, Mac};
use serde::{Deserialize, Serialize};
use sha2::Sha256;
use subtle::ConstantTimeEq;

use crate::managers::{Manager, ManagerContext};

const CHALLENGE_TTL: Duration = Duration::from_secs(5 * 60);
const MAX_CHALLENGES_PER_IP: usize = 16;
const MAX_CHALLENGES: usize = 4096;

const MAX_LOGIN_FAILURES: u32 = 5;
const MAX_TRACKED_IPS: usize = 10_000;
const LOGIN_FAILURE_WINDOW: Duration = Duration::from_secs(10 * 60);
const LOGIN_BAN_PERIOD: Duration = Duration::from_secs(10 * 60);
const LOGIN_CLEANUP_INTERVAL: Duration = Duration::from_secs(60);
const LOGIN_CAPACITY_RETRY_AFTER_SECONDS: u64 = 60;

const JWT_ISSUER: &str = "opanel";
const JWT_KEY_ID: &str = "accessKey";
const JWT_LIFETIME_SECONDS: u64 = 24 * 60 * 60;
const JWT_ALGORITHM: &str = "HS256";
const JWT_TYPE: &str = "JWT";
const JWT_SIGNING_KEY_BYTES: usize = 32;

type HmacSha256 = Hmac<Sha256>;

/// Process-local authentication state shared by HTTP and WebSocket authentication.
///
/// The signing key deliberately exists only in memory, so every restart invalidates all panel
/// sessions even when the persisted credential is unchanged.
pub(crate) struct AuthManager {
    context: ManagerContext,
    signing_key: [u8; JWT_SIGNING_KEY_BYTES],
    challenges: Mutex<ChallengeStore>,
    login_attempts: Mutex<LoginAttemptTracker>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) enum ChallengeCreateResult {
    Created { challenge: String },
    Duplicate,
    CapacityFull { retry_after_seconds: u64 },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum LoginAttemptResult {
    Allowed { failed_attempts: u32 },
    Banned { retry_after_seconds: u64 },
    CapacityFull { retry_after_seconds: u64 },
}

impl AuthManager {
    pub(crate) fn new(context: ManagerContext) -> Self {
        let mut signing_key = [0_u8; JWT_SIGNING_KEY_BYTES];
        getrandom::fill(&mut signing_key)
            .expect("the operating system must provide randomness for the JWT signing key");
        Self::with_signing_key(context, signing_key)
    }

    fn with_signing_key(context: ManagerContext, signing_key: [u8; JWT_SIGNING_KEY_BYTES]) -> Self {
        Self {
            context,
            signing_key,
            challenges: Mutex::new(ChallengeStore::default()),
            login_attempts: Mutex::new(LoginAttemptTracker::new(Instant::now())),
        }
    }

    pub(crate) fn create_challenge(&self, ip: &str, id: &str) -> ChallengeCreateResult {
        lock_or_recover(&self.challenges).create(ip, id, Instant::now())
    }

    /// Consumes a challenge even when the caller later supplies an invalid CRAM result.
    pub(crate) fn consume_challenge(&self, ip: &str, id: &str) -> Option<String> {
        lock_or_recover(&self.challenges).consume(ip, id, Instant::now())
    }

    pub(crate) fn check_login(&self, ip: &str) -> LoginAttemptResult {
        lock_or_recover(&self.login_attempts).check(ip, Instant::now())
    }

    pub(crate) fn record_login_failure(&self, ip: &str) -> LoginAttemptResult {
        lock_or_recover(&self.login_attempts).record_failure(ip, Instant::now())
    }

    pub(crate) fn record_login_success(&self, ip: &str) {
        lock_or_recover(&self.login_attempts).record_success(ip);
    }

    pub(crate) fn verify_cram_result(provided: &str, expected: &str) -> bool {
        bool::from(provided.as_bytes().ct_eq(expected.as_bytes()))
    }

    pub(crate) fn issue_token(&self, access_key: &str, salt: &str) -> String {
        self.issue_token_at(access_key, salt, unix_timestamp())
    }

    pub(crate) fn verify_token(&self, token: &str, access_key: &str, salt: &str) -> bool {
        if access_key.is_empty() || salt.is_empty() {
            return false;
        }
        self.verify_token_at(token, access_key, salt, unix_timestamp())
    }

    fn issue_token_at(&self, access_key: &str, salt: &str, issued_at: u64) -> String {
        let header = JwtHeader {
            algorithm: JWT_ALGORITHM,
            key_id: JWT_KEY_ID,
            token_type: JWT_TYPE,
        };
        let claims = JwtClaims {
            issuer: JWT_ISSUER.to_owned(),
            issued_at,
            expiration: issued_at.saturating_add(JWT_LIFETIME_SECONDS),
            access: access_claim(access_key, salt),
        };

        let encoded_header = encode_json(&header);
        let encoded_claims = encode_json(&claims);
        let signing_input = format!("{encoded_header}.{encoded_claims}");
        let signature = self.sign(signing_input.as_bytes());

        format!("{signing_input}.{}", URL_SAFE_NO_PAD.encode(signature))
    }

    fn verify_token_at(&self, token: &str, access_key: &str, salt: &str, now: u64) -> bool {
        let mut segments = token.split('.');
        let (Some(encoded_header), Some(encoded_claims), Some(encoded_signature)) =
            (segments.next(), segments.next(), segments.next())
        else {
            return false;
        };
        if segments.next().is_some()
            || encoded_header.is_empty()
            || encoded_claims.is_empty()
            || encoded_signature.is_empty()
        {
            return false;
        }

        let Ok(signature) = URL_SAFE_NO_PAD.decode(encoded_signature) else {
            return false;
        };
        let signing_input = format!("{encoded_header}.{encoded_claims}");
        if !self.verify_signature(signing_input.as_bytes(), &signature) {
            return false;
        }

        let Some(header) = decode_json::<JwtHeaderOwned>(encoded_header) else {
            return false;
        };
        if header.algorithm != JWT_ALGORITHM || header.key_id != JWT_KEY_ID {
            return false;
        }

        let Some(claims) = decode_json::<JwtClaims>(encoded_claims) else {
            return false;
        };
        if claims.issuer != JWT_ISSUER
            || claims.issued_at > claims.expiration
            || now >= claims.expiration
        {
            return false;
        }

        let expected_access = access_claim(access_key, salt);
        bool::from(claims.access.as_bytes().ct_eq(expected_access.as_bytes()))
    }

    fn sign(&self, input: &[u8]) -> [u8; 32] {
        let mut mac = <HmacSha256 as Mac>::new_from_slice(&self.signing_key)
            .expect("HMAC-SHA256 accepts keys of every size");
        mac.update(input);
        mac.finalize().into_bytes().into()
    }

    fn verify_signature(&self, input: &[u8], signature: &[u8]) -> bool {
        let mut mac = <HmacSha256 as Mac>::new_from_slice(&self.signing_key)
            .expect("HMAC-SHA256 accepts keys of every size");
        mac.update(input);
        mac.verify_slice(signature).is_ok()
    }
}

impl Manager for AuthManager {
    fn name(&self) -> &'static str {
        "auth"
    }

    fn context(&self) -> &ManagerContext {
        &self.context
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct ChallengeKey {
    ip: String,
    id: String,
}

struct ChallengeRecord {
    challenge: String,
    expires_at: Instant,
}

#[derive(Default)]
struct ChallengeStore {
    challenges: HashMap<ChallengeKey, ChallengeRecord>,
}

impl ChallengeStore {
    fn create(&mut self, ip: &str, id: &str, now: Instant) -> ChallengeCreateResult {
        self.cleanup_expired(now);

        let key = ChallengeKey {
            ip: ip.to_owned(),
            id: id.to_owned(),
        };
        if self.challenges.contains_key(&key) {
            return ChallengeCreateResult::Duplicate;
        }

        let ip_challenge_count = self.challenges.keys().filter(|key| key.ip == ip).count();
        if ip_challenge_count >= MAX_CHALLENGES_PER_IP {
            return ChallengeCreateResult::CapacityFull {
                retry_after_seconds: self.retry_after_seconds(Some(ip), now),
            };
        }
        if self.challenges.len() >= MAX_CHALLENGES {
            return ChallengeCreateResult::CapacityFull {
                retry_after_seconds: self.retry_after_seconds(None, now),
            };
        }

        let challenge = loop {
            let candidate = random_challenge();
            if self
                .challenges
                .values()
                .all(|record| record.challenge != candidate)
            {
                break candidate;
            }
        };
        self.challenges.insert(
            key,
            ChallengeRecord {
                challenge: challenge.clone(),
                expires_at: now + CHALLENGE_TTL,
            },
        );
        ChallengeCreateResult::Created { challenge }
    }

    fn consume(&mut self, ip: &str, id: &str, now: Instant) -> Option<String> {
        self.cleanup_expired(now);
        self.challenges
            .remove(&ChallengeKey {
                ip: ip.to_owned(),
                id: id.to_owned(),
            })
            .map(|record| record.challenge)
    }

    fn cleanup_expired(&mut self, now: Instant) {
        self.challenges.retain(|_, record| record.expires_at > now);
    }

    fn retry_after_seconds(&self, ip: Option<&str>, now: Instant) -> u64 {
        self.challenges
            .iter()
            .filter(|(key, _)| ip.is_none_or(|ip| key.ip == ip))
            .map(|(_, record)| seconds_until(record.expires_at, now))
            .min()
            .unwrap_or(1)
    }
}

#[derive(Clone, Copy)]
struct LoginAttemptRecord {
    failed_attempts: u32,
    window_started_at: Instant,
    banned_until: Option<Instant>,
}

struct LoginAttemptTracker {
    records: HashMap<String, LoginAttemptRecord>,
    next_cleanup_at: Instant,
}

impl LoginAttemptTracker {
    fn new(now: Instant) -> Self {
        Self {
            records: HashMap::new(),
            next_cleanup_at: now + LOGIN_CLEANUP_INTERVAL,
        }
    }

    fn check(&mut self, ip: &str, now: Instant) -> LoginAttemptResult {
        self.cleanup_expired_if_due(now);

        let Some(record) = self.active_record(ip, now) else {
            return if self.records.len() >= MAX_TRACKED_IPS {
                LoginAttemptResult::CapacityFull {
                    retry_after_seconds: LOGIN_CAPACITY_RETRY_AFTER_SECONDS,
                }
            } else {
                LoginAttemptResult::Allowed { failed_attempts: 0 }
            };
        };

        if let Some(banned_until) = record.banned_until
            && banned_until > now
        {
            return LoginAttemptResult::Banned {
                retry_after_seconds: seconds_until(banned_until, now),
            };
        }

        LoginAttemptResult::Allowed {
            failed_attempts: record.failed_attempts,
        }
    }

    fn record_failure(&mut self, ip: &str, now: Instant) -> LoginAttemptResult {
        self.cleanup_expired_if_due(now);

        let record = match self.active_record(ip, now) {
            Some(record) => record,
            None if self.records.len() >= MAX_TRACKED_IPS => {
                return LoginAttemptResult::CapacityFull {
                    retry_after_seconds: LOGIN_CAPACITY_RETRY_AFTER_SECONDS,
                };
            }
            None => LoginAttemptRecord {
                failed_attempts: 0,
                window_started_at: now,
                banned_until: None,
            },
        };

        if let Some(banned_until) = record.banned_until
            && banned_until > now
        {
            return LoginAttemptResult::Banned {
                retry_after_seconds: seconds_until(banned_until, now),
            };
        }

        let failed_attempts = record.failed_attempts + 1;
        let window_started_at = if record.failed_attempts == 0 {
            now
        } else {
            record.window_started_at
        };
        let banned_until =
            (failed_attempts >= MAX_LOGIN_FAILURES).then_some(now + LOGIN_BAN_PERIOD);
        self.records.insert(
            ip.to_owned(),
            LoginAttemptRecord {
                failed_attempts,
                window_started_at,
                banned_until,
            },
        );

        LoginAttemptResult::Allowed { failed_attempts }
    }

    fn record_success(&mut self, ip: &str) {
        self.records.remove(ip);
    }

    fn active_record(&mut self, ip: &str, now: Instant) -> Option<LoginAttemptRecord> {
        let record = self.records.get(ip).copied()?;
        if is_login_record_expired(record, now) {
            self.records.remove(ip);
            None
        } else {
            Some(record)
        }
    }

    fn cleanup_expired_if_due(&mut self, now: Instant) {
        if now < self.next_cleanup_at {
            return;
        }

        self.records
            .retain(|_, record| !is_login_record_expired(*record, now));
        self.next_cleanup_at = now + LOGIN_CLEANUP_INTERVAL;
    }
}

#[derive(Serialize)]
struct JwtHeader<'a> {
    #[serde(rename = "alg")]
    algorithm: &'a str,
    #[serde(rename = "kid")]
    key_id: &'a str,
    #[serde(rename = "typ")]
    token_type: &'a str,
}

#[derive(Deserialize)]
struct JwtHeaderOwned {
    #[serde(rename = "alg")]
    algorithm: String,
    #[serde(rename = "kid")]
    key_id: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct JwtClaims {
    #[serde(rename = "iss")]
    issuer: String,
    #[serde(rename = "iat")]
    issued_at: u64,
    #[serde(rename = "exp")]
    expiration: u64,
    access: String,
}

fn random_challenge() -> String {
    let mut random = [0_u8; 16];
    getrandom::fill(&mut random)
        .expect("the operating system must provide randomness for login challenges");
    to_lower_hex(&random)
}

fn to_lower_hex(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut encoded = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        encoded.push(HEX[usize::from(byte >> 4)] as char);
        encoded.push(HEX[usize::from(byte & 0x0f)] as char);
    }
    encoded
}

fn access_claim(access_key: &str, salt: &str) -> String {
    format!("{:x}", md5::compute(format!("{salt}{access_key}")))
}

fn encode_json<T: Serialize>(value: &T) -> String {
    let json = serde_json::to_vec(value).expect("JWT header and claims must be serializable");
    URL_SAFE_NO_PAD.encode(json)
}

fn decode_json<T: for<'de> Deserialize<'de>>(encoded: &str) -> Option<T> {
    let json = URL_SAFE_NO_PAD.decode(encoded).ok()?;
    serde_json::from_slice(&json).ok()
}

fn is_login_record_expired(record: LoginAttemptRecord, now: Instant) -> bool {
    match record.banned_until {
        Some(banned_until) => now >= banned_until,
        None => now.saturating_duration_since(record.window_started_at) >= LOGIN_FAILURE_WINDOW,
    }
}

fn seconds_until(target: Instant, now: Instant) -> u64 {
    let remaining = target.saturating_duration_since(now);
    let rounded_up = remaining
        .as_secs()
        .saturating_add(u64::from(remaining.subsec_nanos() != 0));
    rounded_up.max(1)
}

fn unix_timestamp() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

fn lock_or_recover<T>(mutex: &Mutex<T>) -> MutexGuard<'_, T> {
    mutex.lock().unwrap_or_else(PoisonError::into_inner)
}

#[cfg(test)]
mod tests {
    use std::sync::Weak;

    use serde_json::Value;
    use tokio_util::sync::CancellationToken;

    use super::*;

    const TEST_ACCESS_KEY: &str = "0123456789abcdef0123456789abcdef";
    const TEST_SALT: &str = "abc123";

    fn manager_with_key(key: u8) -> AuthManager {
        AuthManager::with_signing_key(
            ManagerContext::new(Weak::new(), CancellationToken::new()),
            [key; JWT_SIGNING_KEY_BYTES],
        )
    }

    fn created_challenge(result: ChallengeCreateResult) -> String {
        let ChallengeCreateResult::Created { challenge } = result else {
            panic!("expected a newly-created challenge");
        };
        challenge
    }

    #[test]
    fn challenges_are_unique_hex_ip_bound_and_single_use() {
        let auth = manager_with_key(1);
        let first = created_challenge(auth.create_challenge("192.0.2.1", "browser"));
        let second = created_challenge(auth.create_challenge("192.0.2.1", "other"));

        assert_eq!(first.len(), 32);
        assert!(first.bytes().all(|byte| byte.is_ascii_hexdigit()));
        assert_eq!(first, first.to_ascii_lowercase());
        assert_ne!(first, second);
        assert_eq!(
            auth.create_challenge("192.0.2.1", "browser"),
            ChallengeCreateResult::Duplicate
        );

        assert_eq!(auth.consume_challenge("192.0.2.2", "browser"), None);
        assert_eq!(auth.consume_challenge("192.0.2.1", "browser"), Some(first));
        assert_eq!(auth.consume_challenge("192.0.2.1", "browser"), None);
    }

    #[test]
    fn challenge_capacity_is_enforced_per_ip_before_global_capacity() {
        let auth = manager_with_key(2);
        for index in 0..MAX_CHALLENGES_PER_IP {
            assert!(matches!(
                auth.create_challenge("192.0.2.1", &index.to_string()),
                ChallengeCreateResult::Created { .. }
            ));
        }
        assert!(matches!(
            auth.create_challenge("192.0.2.1", "overflow"),
            ChallengeCreateResult::CapacityFull {
                retry_after_seconds: 1..=300
            }
        ));

        let now = Instant::now();
        let mut store = lock_or_recover(&auth.challenges);
        store.challenges.clear();
        for index in 0..MAX_CHALLENGES {
            store.challenges.insert(
                ChallengeKey {
                    ip: format!("source-{index}"),
                    id: "id".to_owned(),
                },
                ChallengeRecord {
                    challenge: format!("{index:032x}"),
                    expires_at: now + CHALLENGE_TTL,
                },
            );
        }
        assert!(matches!(
            store.create("new-source", "id", now),
            ChallengeCreateResult::CapacityFull {
                retry_after_seconds: 300
            }
        ));
    }

    #[test]
    fn expired_challenges_are_removed_before_create_and_consume() {
        let auth = manager_with_key(3);
        let now = Instant::now();
        let mut store = lock_or_recover(&auth.challenges);
        let challenge = created_challenge(store.create("192.0.2.1", "id", now));

        assert_eq!(store.consume("192.0.2.1", "id", now + CHALLENGE_TTL), None);
        assert!(
            !store
                .challenges
                .values()
                .any(|record| record.challenge == challenge)
        );
        assert!(matches!(
            store.create("192.0.2.1", "id", now + CHALLENGE_TTL),
            ChallengeCreateResult::Created { .. }
        ));
    }

    #[test]
    fn fifth_failure_is_allowed_and_subsequent_requests_are_banned() {
        let auth = manager_with_key(4);
        let ip = "192.0.2.1";

        assert_eq!(
            auth.check_login(ip),
            LoginAttemptResult::Allowed { failed_attempts: 0 }
        );
        for failed_attempts in 1..=MAX_LOGIN_FAILURES {
            assert_eq!(
                auth.record_login_failure(ip),
                LoginAttemptResult::Allowed { failed_attempts }
            );
        }
        assert!(matches!(
            auth.check_login(ip),
            LoginAttemptResult::Banned {
                retry_after_seconds: 1..=600
            }
        ));
        assert!(matches!(
            auth.record_login_failure(ip),
            LoginAttemptResult::Banned { .. }
        ));

        auth.record_login_success(ip);
        assert_eq!(
            auth.check_login(ip),
            LoginAttemptResult::Allowed { failed_attempts: 0 }
        );
    }

    #[test]
    fn login_windows_bans_and_capacity_expire_with_java_semantics() {
        let now = Instant::now();
        let mut tracker = LoginAttemptTracker::new(now);
        assert_eq!(
            tracker.record_failure("window", now),
            LoginAttemptResult::Allowed { failed_attempts: 1 }
        );
        assert_eq!(
            tracker.check("window", now + LOGIN_FAILURE_WINDOW),
            LoginAttemptResult::Allowed { failed_attempts: 0 }
        );

        for failed_attempts in 1..=MAX_LOGIN_FAILURES {
            assert_eq!(
                tracker.record_failure("banned", now),
                LoginAttemptResult::Allowed { failed_attempts }
            );
        }
        assert_eq!(
            tracker.check("banned", now + LOGIN_BAN_PERIOD),
            LoginAttemptResult::Allowed { failed_attempts: 0 }
        );

        tracker.records.clear();
        tracker.next_cleanup_at = now + LOGIN_CLEANUP_INTERVAL;
        for index in 0..MAX_TRACKED_IPS {
            tracker.records.insert(
                format!("source-{index}"),
                LoginAttemptRecord {
                    failed_attempts: 1,
                    window_started_at: now,
                    banned_until: None,
                },
            );
        }
        assert_eq!(
            tracker.check("new-source", now),
            LoginAttemptResult::CapacityFull {
                retry_after_seconds: LOGIN_CAPACITY_RETRY_AFTER_SECONDS
            }
        );
    }

    #[test]
    fn issued_jwt_has_expected_header_and_claims() {
        let auth = manager_with_key(5);
        let issued_at = 1_700_000_000;
        let token = auth.issue_token_at(TEST_ACCESS_KEY, TEST_SALT, issued_at);
        let mut segments = token.split('.');
        let header: Value = decode_json(segments.next().unwrap()).unwrap();
        let claims: Value = decode_json(segments.next().unwrap()).unwrap();

        assert_eq!(header["alg"], JWT_ALGORITHM);
        assert_eq!(header["kid"], JWT_KEY_ID);
        assert_eq!(header["typ"], JWT_TYPE);
        assert_eq!(claims["iss"], JWT_ISSUER);
        assert_eq!(claims["iat"], issued_at);
        assert_eq!(claims["exp"], issued_at + JWT_LIFETIME_SECONDS);
        assert_eq!(claims["access"], access_claim(TEST_ACCESS_KEY, TEST_SALT));
        assert!(auth.verify_token_at(
            &token,
            TEST_ACCESS_KEY,
            TEST_SALT,
            issued_at + JWT_LIFETIME_SECONDS - 1
        ));
        assert!(!auth.verify_token_at(
            &token,
            TEST_ACCESS_KEY,
            TEST_SALT,
            issued_at + JWT_LIFETIME_SECONDS
        ));
    }

    #[test]
    fn jwt_rejects_tampering_config_changes_and_restart_keys() {
        let auth = manager_with_key(6);
        let restarted = manager_with_key(7);
        let issued_at = 1_700_000_000;
        let token = auth.issue_token_at(TEST_ACCESS_KEY, TEST_SALT, issued_at);

        assert!(!auth.verify_token_at(&token, "wrong", TEST_SALT, issued_at));
        assert!(!auth.verify_token_at(&token, TEST_ACCESS_KEY, "wrong", issued_at));
        assert!(!restarted.verify_token_at(&token, TEST_ACCESS_KEY, TEST_SALT, issued_at));

        let mut tampered = token.into_bytes();
        let index = tampered.len() - 1;
        tampered[index] = if tampered[index] == b'A' { b'B' } else { b'A' };
        assert!(!auth.verify_token_at(
            std::str::from_utf8(&tampered).unwrap(),
            TEST_ACCESS_KEY,
            TEST_SALT,
            issued_at
        ));

        let valid = auth.issue_token_at(TEST_ACCESS_KEY, TEST_SALT, issued_at);
        assert!(!auth.verify_token(&valid, "", TEST_SALT));
        assert!(!auth.verify_token(&valid, TEST_ACCESS_KEY, ""));
    }

    #[test]
    fn cram_comparison_requires_an_exact_match() {
        assert!(AuthManager::verify_cram_result(
            "0123456789abcdef0123456789abcdef",
            "0123456789abcdef0123456789abcdef"
        ));
        assert!(!AuthManager::verify_cram_result(
            "1123456789abcdef0123456789abcdef",
            "0123456789abcdef0123456789abcdef"
        ));
        assert!(!AuthManager::verify_cram_result("short", "different"));
    }
}
