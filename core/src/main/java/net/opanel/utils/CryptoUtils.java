package net.opanel.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class for AES-256-CBC encryption and decryption.
 * Uses PBKDF2 to derive keys from a salt string.
 */
public class CryptoUtils {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final String FIXED_SALT = "OPanelCrypto";

    /**
     * Encrypts plaintext using AES-256-CBC.
     * The result is Base64 encoded and includes the IV prepended to the ciphertext.
     *
     * @param plaintext The text to encrypt
     * @param salt      The salt used for key derivation (typically from
     *                  OPanelConfiguration.salt)
     * @return Base64 encoded string containing IV + ciphertext, or empty string if
     *         input is empty
     */
    public static String encrypt(String plaintext, String salt) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }

        try {
            SecretKey key = deriveKey(salt);

            // Generate random IV
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts ciphertext that was encrypted using the encrypt method.
     *
     * @param ciphertext Base64 encoded string containing IV + ciphertext
     * @param salt       The salt used for key derivation (must match the salt used
     *                   for encryption)
     * @return The decrypted plaintext, or empty string if input is empty
     */
    public static String decrypt(String ciphertext, String salt) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return "";
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }

        try {
            SecretKey key = deriveKey(salt);

            // Decode and split IV + ciphertext
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] plaintext = cipher.doFinal(encryptedBytes);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Derives an AES-256 key from the given salt using PBKDF2.
     */
    private static SecretKey deriveKey(String salt) throws Exception {
        // Combine user salt with fixed salt for additional entropy
        String combinedSalt = FIXED_SALT + salt;

        KeySpec spec = new PBEKeySpec(
                salt.toCharArray(),
                combinedSalt.getBytes(StandardCharsets.UTF_8),
                ITERATIONS,
                KEY_LENGTH);

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * Checks if a string appears to be encrypted (is valid Base64 and has minimum
     * length).
     *
     * @param text The text to check
     * @return true if the text appears to be encrypted
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return decoded.length > IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
