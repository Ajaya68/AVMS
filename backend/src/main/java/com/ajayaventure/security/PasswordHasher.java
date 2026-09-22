package com.ajayaventure.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing using PBKDF2-HMAC-SHA256 with a per-user random salt.
 *
 * <p>Stored format: {@code pbkdf2-sha256$iterations$saltBase64$hashBase64}.</p>
 *
 * <p>Plaintext passwords are never stored or logged. Iteration count comes from
 * configuration so it can be raised over time.</p>
 */
public final class PasswordHasher {

    public static final String PREFIX = "pbkdf2-sha256";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int DEFAULT_ITERATIONS = 210_000;

    private PasswordHasher() {
    }

    public static int iterations() {
        int requested = com.ajayaventure.util.EnvVars.getInt("PASSWORD_ITERATIONS", DEFAULT_ITERATIONS);
        return requested > 0 ? requested : DEFAULT_ITERATIONS;
    }

    public static String hash(String plaintext) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return hash(plaintext, salt, iterations());
    }

    /** Returns a stored-format string for the given salt/iterations (test helper). */
    public static String hash(String plaintext, byte[] salt, int iterations) {
        byte[] key = derive(plaintext, salt, iterations);
        return PREFIX + "$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    /**
     * Constant-time verification of a candidate password against a stored hash.
     * Unknown/malformed stored hashes always verify as false.
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || plaintext.isEmpty()) {
            return false;
        }
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(plaintext, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String plaintext, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(plaintext.toCharArray(), salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}