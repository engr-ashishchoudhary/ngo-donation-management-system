package com.ngo.donation.security;

public record VerifiedJwtClaims(Integer id, String email, String role) {

    public boolean isValid() {
        return id != null && email != null && !email.isBlank() && role != null && !role.isBlank();
    }
}
