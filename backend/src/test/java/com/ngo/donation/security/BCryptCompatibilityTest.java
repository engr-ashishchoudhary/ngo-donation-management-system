package com.ngo.donation.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class BCryptCompatibilityTest {

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Cost 4 is the minimum allowed by BCrypt and is fast enough for unit tests.
        // Production uses cost 10 (configured via Spring's BCryptPasswordEncoder bean).
        passwordEncoder = new BCryptPasswordEncoder(4);
    }

    @Test
    @DisplayName("Should encode password with valid $2a$ format")
    void testEncodeGeneratesValidBcrypt() {
        String rawPassword = "TestSecurePassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertEquals(60, encoded.length());
        assertTrue(encoded.startsWith("$2a$"));
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("Should verify password against standard $2a$ hash")
    void testMatches2aFormat() {
        String rawPassword = "password";
        String hash2a = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, hash2a));
        assertFalse(passwordEncoder.matches("wrongpassword", hash2a));
    }

    @Test
    @DisplayName("Should verify password against standard $2b$ hash")
    void testMatches2bFormat() {
        String rawPassword = "password";
        String hash2a = passwordEncoder.encode(rawPassword);
        String hash2b = hash2a.replaceFirst("^\\$2a\\$", "\\$2b\\$");
        assertTrue(passwordEncoder.matches(rawPassword, hash2b));
        assertFalse(passwordEncoder.matches("wrongpassword", hash2b));
    }

    @Test
    @DisplayName("Should verify password against standard $2y$ hash")
    void testMatches2yFormat() {
        String rawPassword = "password";
        String hash2a = passwordEncoder.encode(rawPassword);
        String hash2y = hash2a.replaceFirst("^\\$2a\\$", "\\$2y\\$");
        assertTrue(passwordEncoder.matches(rawPassword, hash2y));
        assertFalse(passwordEncoder.matches("wrongpassword", hash2y));
    }

    @Test
    @DisplayName("Should fail matching for unsupported or corrupted hash formats")
    void testRejectsCorruptedOrUnsupportedFormats() {
        // Plain text
        assertFalse(passwordEncoder.matches("password", "plaintext_password"));

        // Truncated hash (not 60 chars)
        assertFalse(passwordEncoder.matches("password", "$2a$10$shortHash"));

        // MD5 or SHA-256 hash
        assertFalse(passwordEncoder.matches("password", "5f4dcc3b5aa765d61d8327deb882cf99"));

        // Placeholder hash (58 chars with invalid base64 payload)
        String placeholder = "$2a$10$PH.aHGFakeBcryptHashAdminOneXXXXXXXXXXXXXXXXXXXXXXX";
        assertFalse(passwordEncoder.matches("password", placeholder));
    }
}
