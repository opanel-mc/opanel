package net.opanel.web;

import at.favre.lib.crypto.bcrypt.BCrypt;
import net.opanel.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

public final class AccessKeyHasher {
    public static final int MIN_KEY_LENGTH = 6;
    // Legacy versions did not enforce a maximum key length, so this limit only applies
    // when setting a new key to avoid locking out admins with existing >256-char keys.
    public static final int MAX_NEW_KEY_LENGTH = 256;

    private static final String LEGACY_WRAP_PREFIX = "md5-legacy:";
    private static final Pattern RAW_MD5_PATTERN = Pattern.compile("^[0-9a-f]{32}$");
    private static final int BCRYPT_COST = 12;
    private static final int MAX_CONCURRENT_KDF = 4;
    private static final int MAX_QUEUED_KDF = 8;
    private static final Semaphore KDF_SEMAPHORE = new Semaphore(MAX_CONCURRENT_KDF, true);
    private static final Semaphore REQUEST_SLOT_SEMAPHORE = new Semaphore(MAX_CONCURRENT_KDF + MAX_QUEUED_KDF, true);

    public enum VerifyResult {
        INVALID,
        VALID,
        VALID_NEEDS_UPGRADE
    }

    public static final class RequestSlot implements AutoCloseable {
        private boolean closed = false;

        private RequestSlot() {}

        @Override
        public void close() {
            if(!closed) {
                closed = true;
                REQUEST_SLOT_SEMAPHORE.release();
            }
        }
    }

    private AccessKeyHasher() {}

    public static RequestSlot tryAcquireRequestSlot() {
        if(!REQUEST_SLOT_SEMAPHORE.tryAcquire()) {
            return null;
        }
        return new RequestSlot();
    }

    public static boolean isValidInputLength(String key) {
        return key != null && !key.isEmpty();
    }

    public static boolean isValidNewKeyLength(String key) {
        return key != null && key.length() >= MIN_KEY_LENGTH && key.length() <= MAX_NEW_KEY_LENGTH;
    }

    public static String hash(String plaintextKey) {
        if(!isValidInputLength(plaintextKey)) {
            throw new IllegalArgumentException("Invalid access key length.");
        }
        String prehashed = sha256Base64(plaintextKey);
        return bcryptHash(prehashed);
    }

    public static String wrapLegacyIfNeeded(String storedKey) {
        if(storedKey != null && RAW_MD5_PATTERN.matcher(storedKey).matches()) {
            return LEGACY_WRAP_PREFIX + bcryptHash(storedKey);
        }
        return null;
    }

    public static VerifyResult verify(String plaintextInput, String storedKey) {
        if(!isValidInputLength(plaintextInput) || storedKey == null || storedKey.isBlank()) {
            return VerifyResult.INVALID;
        }

        if(storedKey.startsWith(LEGACY_WRAP_PREFIX)) {
            String wrappedBcrypt = storedKey.substring(LEGACY_WRAP_PREFIX.length());
            if(!isBcryptHash(wrappedBcrypt)) return VerifyResult.INVALID;

            String legacyMd5 = Utils.md5(Utils.md5(plaintextInput));
            return bcryptVerify(legacyMd5, wrappedBcrypt)
                    ? VerifyResult.VALID_NEEDS_UPGRADE
                    : VerifyResult.INVALID;
        }

        if(RAW_MD5_PATTERN.matcher(storedKey).matches()) {
            String legacyMd5 = Utils.md5(Utils.md5(plaintextInput));
            boolean matched = MessageDigest.isEqual(
                    legacyMd5.getBytes(StandardCharsets.US_ASCII),
                    storedKey.getBytes(StandardCharsets.US_ASCII)
            );
            return matched ? VerifyResult.VALID_NEEDS_UPGRADE : VerifyResult.INVALID;
        }

        if(isBcryptHash(storedKey)) {
            String prehashed = sha256Base64(plaintextInput);
            return bcryptVerify(prehashed, storedKey)
                    ? VerifyResult.VALID
                    : VerifyResult.INVALID;
        }

        return VerifyResult.INVALID;
    }

    private static boolean isBcryptHash(String value) {
        return value.startsWith("$2b$") || value.startsWith("$2a$") || value.startsWith("$2y$");
    }

    private static String sha256Base64(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is unavailable", e);
        }
    }

    private static String bcryptHash(String input) {
        KDF_SEMAPHORE.acquireUninterruptibly();
        try {
            return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(BCRYPT_COST, input.toCharArray());
        } finally {
            KDF_SEMAPHORE.release();
        }
    }

    private static boolean bcryptVerify(String input, String bcryptHash) {
        KDF_SEMAPHORE.acquireUninterruptibly();
        try {
            return BCrypt.verifyer().verify(input.toCharArray(), bcryptHash.toCharArray()).verified;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            KDF_SEMAPHORE.release();
        }
    }
}
