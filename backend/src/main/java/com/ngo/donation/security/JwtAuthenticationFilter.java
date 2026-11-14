package com.ngo.donation.security;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;
    private final DonorRepository donorRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   AdminRepository adminRepository,
                                   DonorRepository donorRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminRepository = adminRepository;
        this.donorRepository = donorRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            Optional<VerifiedJwtClaims> claimsOpt = jwtTokenProvider.verifyAndExtractClaims(token);
            if (claimsOpt.isPresent()) {
                VerifiedJwtClaims claims = claimsOpt.get();
                UserPrincipal principal = authenticateAgainstDatabase(claims);
                if (principal != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Database authoritative check failed for claims: id={}, role={}", claims.id(), claims.role());
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.warn("JWT token verification or extraction returned empty claims");
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserPrincipal authenticateAgainstDatabase(VerifiedJwtClaims claims) {
        Integer id = claims.id();
        String email = claims.email();
        String rawRole = claims.role();

        if (rawRole == null || id == null || email == null) {
            return null;
        }

        String role = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;

        if ("ROLE_DONOR".equalsIgnoreCase(role)) {
            Optional<Donor> donorOpt = donorRepository.findById(id);
            if (donorOpt.isPresent()) {
                Donor donor = donorOpt.get();
                if (Boolean.TRUE.equals(donor.getIsActive()) && email.equalsIgnoreCase(donor.getEmail())) {
                    return new UserPrincipal(donor.getDonorId(), donor.getEmail(), "ROLE_DONOR");
                }
            }
            return null;
        }

        Optional<Admin> adminOpt = adminRepository.findById(id);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            String adminRoleString = "ROLE_" + admin.getRole().name();
            if (Boolean.TRUE.equals(admin.getIsActive()) &&
                    email.equalsIgnoreCase(admin.getEmail()) &&
                    adminRoleString.equalsIgnoreCase(role)) {
                return new UserPrincipal(admin.getAdminId(), admin.getEmail(), adminRoleString);
            }
        }

        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
