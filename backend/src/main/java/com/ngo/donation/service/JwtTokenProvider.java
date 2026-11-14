package com.ngo.donation.service;

public interface JwtTokenProvider {

    String generateToken(Integer id, String email, String role);

    String getEmailFromToken(String token);

    boolean validateToken(String token);
}
