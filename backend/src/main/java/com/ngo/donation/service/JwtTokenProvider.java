package com.ngo.donation.service;

import com.ngo.donation.security.VerifiedJwtClaims;
import java.util.Optional;

public interface JwtTokenProvider {

    String generateToken(Integer id, String email, String role);

    Optional<VerifiedJwtClaims> verifyAndExtractClaims(String token);

    String getEmailFromToken(String token);

    Integer getIdFromToken(String token);

    String getRoleFromToken(String token);

    boolean validateToken(String token);
}
