package com.ngo.donation.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderImplTest {

    private static final String VALID_SECRET = "VerySecureSecretKeyForTestingPurposesMustBeAtLeast32Bytes!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProviderImpl jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProviderImpl(VALID_SECRET, EXPIRATION_MS);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate token and extract valid claims in single pass")
    void testGenerateAndExtractClaims() {
        Integer id = 42;
        String email = "donor@example.org";
        String role = "ROLE_DONOR";

        String token = jwtTokenProvider.generateToken(id, email, role);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Optional<VerifiedJwtClaims> claimsOpt = jwtTokenProvider.verifyAndExtractClaims(token);
        assertTrue(claimsOpt.isPresent());

        VerifiedJwtClaims claims = claimsOpt.get();
        assertEquals(id, claims.id());
        assertEquals(email, claims.email());
        assertEquals(role, claims.role());
        assertTrue(claims.isValid());
    }

    @Test
    @DisplayName("Should correctly delegate token claim accessors")
    void testClaimAccessors() {
        Integer id = 101;
        String email = "admin@example.org";
        String role = "ROLE_SUPER_ADMIN";

        String token = jwtTokenProvider.generateToken(id, email, role);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        assertEquals(id, jwtTokenProvider.getIdFromToken(token));
        assertEquals(role, jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectsExpiredToken() {
        // Provider with negative expiration produces already expired token
        JwtTokenProviderImpl expiredProvider = new JwtTokenProviderImpl(VALID_SECRET, -1000L);
        expiredProvider.init();

        String expiredToken = expiredProvider.generateToken(1, "expired@test.org", "ROLE_DONOR");

        Optional<VerifiedJwtClaims> claims = jwtTokenProvider.verifyAndExtractClaims(expiredToken);
        assertTrue(claims.isEmpty());
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertNull(jwtTokenProvider.getEmailFromToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject tampered token")
    void testRejectsTamperedToken() {
        String token = jwtTokenProvider.generateToken(1, "user@test.org", "ROLE_DONOR");
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        Optional<VerifiedJwtClaims> claims = jwtTokenProvider.verifyAndExtractClaims(tamperedToken);
        assertTrue(claims.isEmpty());
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should return empty for null or blank token")
    void testNullOrBlankToken() {
        assertTrue(jwtTokenProvider.verifyAndExtractClaims(null).isEmpty());
        assertTrue(jwtTokenProvider.verifyAndExtractClaims("").isEmpty());
        assertTrue(jwtTokenProvider.verifyAndExtractClaims("   ").isEmpty());
    }

    @Test
    @DisplayName("Should throw IllegalStateException if JWT secret is shorter than 256 bits (32 bytes)")
    void testRejectsWeakSecret() {
        String weakSecret = "short-secret"; // < 32 bytes
        JwtTokenProviderImpl weakProvider = new JwtTokenProviderImpl(weakSecret, EXPIRATION_MS);

        assertThrows(IllegalStateException.class, weakProvider::init);
    }
}
