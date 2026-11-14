package com.ngo.donation.security;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private DonorRepository donorRepository;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider, adminRepository, donorRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate active donor with matching database record")
    void testAuthenticateActiveDonor() throws ServletException, IOException {
        String token = "valid.donor.token";
        VerifiedJwtClaims claims = new VerifiedJwtClaims(1, "donor@ngo.org", "ROLE_DONOR");
        Donor donor = new Donor("Donor Name", "donor@ngo.org", "hash", "1234567890", "Address");
        donor.setDonorId(1);
        donor.setIsActive(true);

        when(jwtTokenProvider.verifyAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(donorRepository.findById(1)).thenReturn(Optional.of(donor));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(1, principal.getId());
        assertEquals("donor@ngo.org", principal.getEmail());
        assertEquals("ROLE_DONOR", principal.getRole());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate active SUPER_ADMIN with matching role and database record")
    void testAuthenticateActiveAdmin() throws ServletException, IOException {
        String token = "valid.admin.token";
        VerifiedJwtClaims claims = new VerifiedJwtClaims(2, "admin@ngo.org", "ROLE_SUPER_ADMIN");
        Admin admin = new Admin("Admin Name", "admin@ngo.org", "hash", AdminRole.SUPER_ADMIN);
        admin.setAdminId(2);
        admin.setIsActive(true);

        when(jwtTokenProvider.verifyAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(adminRepository.findById(2)).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(2, principal.getId());
        assertEquals("admin@ngo.org", principal.getEmail());
        assertEquals("ROLE_SUPER_ADMIN", principal.getRole());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear context when donor account in database is inactive")
    void testRejectsInactiveDonor() throws ServletException, IOException {
        String token = "valid.donor.token";
        VerifiedJwtClaims claims = new VerifiedJwtClaims(1, "donor@ngo.org", "ROLE_DONOR");
        Donor donor = new Donor("Donor Name", "donor@ngo.org", "hash", "1234567890", "Address");
        donor.setDonorId(1);
        donor.setIsActive(false);

        when(jwtTokenProvider.verifyAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(donorRepository.findById(1)).thenReturn(Optional.of(donor));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear context when admin role in token does not match role in database")
    void testRejectsAdminRoleMismatch() throws ServletException, IOException {
        String token = "forged.role.token";
        // Token claims SUPER_ADMIN, but DB record is ADMIN — role mismatch should reject
        VerifiedJwtClaims claims = new VerifiedJwtClaims(3, "planadmin@ngo.org", "ROLE_SUPER_ADMIN");
        Admin admin = new Admin("Plan Admin", "planadmin@ngo.org", "hash", AdminRole.ADMIN);
        admin.setAdminId(3);
        admin.setIsActive(true);

        when(jwtTokenProvider.verifyAndExtractClaims(token)).thenReturn(Optional.of(claims));
        when(adminRepository.findById(3)).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should pass request without authentication when no Authorization header is present")
    void testPassesWithoutHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, adminRepository, donorRepository);
    }

    @Test
    @DisplayName("Should clear context and continue when token verification returns empty claims")
    void testInvalidTokenHandledSafely() throws ServletException, IOException {
        String invalidToken = "invalid.token";
        when(jwtTokenProvider.verifyAndExtractClaims(invalidToken)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
