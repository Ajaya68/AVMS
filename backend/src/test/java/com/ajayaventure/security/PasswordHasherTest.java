package com.ajayaventure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashThenVerifyRoundTrips() {
        String hash = PasswordHasher.hash("s3cret-p@ss");
        assertTrue(hash.startsWith("pbkdf2-sha256$"));
        assertTrue(PasswordHasher.verify("s3cret-p@ss", hash));
    }

    @Test
    void samePasswordProducesDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("pw"), PasswordHasher.hash("pw"));
    }

    @Test
    void wrongPasswordFails() {
        String hash = PasswordHasher.hash("right");
        assertFalse(PasswordHasher.verify("wrong", hash));
    }

    @Test
    void malformedStoredHashFails() {
        assertFalse(PasswordHasher.verify("x", "not-a-hash"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2-sha256$abc$def"));
        assertFalse(PasswordHasher.verify(null, "pbkdf2-sha256$1$YWJj$ZGVm"));
    }
}