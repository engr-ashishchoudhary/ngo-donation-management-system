package com.ngo.donation.security;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPasswordBootstrapRunnerTest {

    @Mock
    private AdminRepository adminRepository;

    private PasswordEncoder passwordEncoder;
    private static final String SUPER_ADMIN_EMAIL = "priya.sharma@sewngo.org";
    private static final String PLACEHOLDER_HASH = "$2a$10$PH.aHGFakeBcryptHashAdminOneXXXXXXXXXXXXXXXXXXXXXXX";

    @BeforeEach
    void setUp() {
        // Cost 4 is the minimum allowed by BCrypt and is fast enough for unit tests.
        // Production uses cost 10 (configured via Spring's BCryptPasswordEncoder bean).
        passwordEncoder = new BCryptPasswordEncoder(4);
    }

    @Test
    @DisplayName("Should successfully bootstrap initial password preserving exact untrimmed characters")
    void testBootstrapPlaceholderPasswordSuccess() {
        String untrimmedInitialPassword = "  PriyaSuperAdminKey2026!  ";
        Admin admin = new Admin("Priya Sharma", SUPER_ADMIN_EMAIL, PLACEHOLDER_HASH, AdminRole.SUPER_ADMIN);

        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                untrimmedInitialPassword,
                SUPER_ADMIN_EMAIL
        );

        runner.bootstrapSuperAdminPassword();

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());

        Admin savedAdmin = adminCaptor.getValue();
        assertNotNull(savedAdmin.getPasswordHash());
        assertNotEquals(PLACEHOLDER_HASH, savedAdmin.getPasswordHash());
        assertTrue(passwordEncoder.matches(untrimmedInitialPassword, savedAdmin.getPasswordHash()));
        // Must NOT match trimmed password because untrimmed was preserved
        assertFalse(passwordEncoder.matches("PriyaSuperAdminKey2026!", savedAdmin.getPasswordHash()));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when placeholder hash is present but initial password is blank")
    void testBootstrapFailsWhenInitialPasswordBlank() {
        Admin admin = new Admin("Priya Sharma", SUPER_ADMIN_EMAIL, PLACEHOLDER_HASH, AdminRole.SUPER_ADMIN);
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "   ",
                SUPER_ADMIN_EMAIL
        );

        assertThrows(IllegalStateException.class, runner::bootstrapSuperAdminPassword);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip bootstrap when SUPER_ADMIN already has a valid 60-char BCrypt hash")
    void testBootstrapSkipsWhenAlreadyValidBcryptHash() {
        String validHash = passwordEncoder.encode("ExistingSecurePassword123");
        Admin admin = new Admin("Priya Sharma", SUPER_ADMIN_EMAIL, validHash, AdminRole.SUPER_ADMIN);
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "NewPasswordThatShouldBeIgnored",
                SUPER_ADMIN_EMAIL
        );

        runner.bootstrapSuperAdminPassword();

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when hash is corrupted or unsupported format")
    void testBootstrapFailsWhenCorruptedHash() {
        Admin admin = new Admin("Priya Sharma", SUPER_ADMIN_EMAIL, "corrupted_plain_text_hash", AdminRole.SUPER_ADMIN);
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "ValidInitialPassword123",
                SUPER_ADMIN_EMAIL
        );

        assertThrows(IllegalStateException.class, runner::bootstrapSuperAdminPassword);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when SUPER_ADMIN account is inactive")
    void testBootstrapFailsWhenAdminInactive() {
        Admin admin = new Admin("Priya Sharma", SUPER_ADMIN_EMAIL, PLACEHOLDER_HASH, AdminRole.SUPER_ADMIN);
        admin.setIsActive(false);
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "ValidInitialPassword123",
                SUPER_ADMIN_EMAIL
        );

        assertThrows(IllegalStateException.class, runner::bootstrapSuperAdminPassword);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when account is not SUPER_ADMIN role")
    void testBootstrapFailsWhenNotSuperAdminRole() {
        // Use AdminRole.ADMIN (not SUPER_ADMIN) — the bootstrap must reject accounts without SUPER_ADMIN role
        Admin admin = new Admin("Staff Admin", SUPER_ADMIN_EMAIL, PLACEHOLDER_HASH, AdminRole.ADMIN);
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "ValidInitialPassword123",
                SUPER_ADMIN_EMAIL
        );

        assertThrows(IllegalStateException.class, runner::bootstrapSuperAdminPassword);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should do nothing gracefully if seeded admin account does not exist")
    void testBootstrapDoesNothingWhenAdminNotFound() {
        when(adminRepository.findByEmail(SUPER_ADMIN_EMAIL)).thenReturn(Optional.empty());

        AdminPasswordBootstrapRunner runner = new AdminPasswordBootstrapRunner(
                adminRepository,
                passwordEncoder,
                "ValidInitialPassword123",
                SUPER_ADMIN_EMAIL
        );

        assertDoesNotThrow(runner::bootstrapSuperAdminPassword);
        verify(adminRepository, never()).save(any());
    }
}
