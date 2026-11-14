package com.ngo.donation.security;

import com.ngo.donation.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderImpl.class);

    private final String secret;
    private final long expirationMs;
    private SecretKey signingKey;

    public JwtTokenProviderImpl(@Value("${app.jwt.secret}") String secret,
                                @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) long.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(Integer id, String email, String role) {
        if (signingKey == null) {
            init();
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<VerifiedJwtClaims> verifyAndExtractClaims(String token) {
        if (signingKey == null) {
            init();
        }
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            Object idObj = claims.get("id");
            Integer id = null;
            if (idObj instanceof Number number) {
                id = number.intValue();
            } else if (idObj instanceof String str) {
                id = Integer.parseInt(str);
            }
            String role = claims.get("role", String.class);

            VerifiedJwtClaims verified = new VerifiedJwtClaims(id, email, role);
            if (verified.isValid()) {
                return Optional.of(verified);
            }
            log.warn("JWT claims validation failed: missing or empty required claims");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT verification failed: token is invalid or expired.");
            return Optional.empty();
        }
    }

    @Override
    public String getEmailFromToken(String token) {
        return verifyAndExtractClaims(token)
                .map(VerifiedJwtClaims::email)
                .orElse(null);
    }

    @Override
    public Integer getIdFromToken(String token) {
        return verifyAndExtractClaims(token)
                .map(VerifiedJwtClaims::id)
                .orElse(null);
    }

    @Override
    public String getRoleFromToken(String token) {
        return verifyAndExtractClaims(token)
                .map(VerifiedJwtClaims::role)
                .orElse(null);
    }

    @Override
    public boolean validateToken(String token) {
        return verifyAndExtractClaims(token).isPresent();
    }
}
