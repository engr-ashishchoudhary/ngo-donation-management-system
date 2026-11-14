package com.ngo.donation.security;

import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class AdminPasswordBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPasswordBootstrapRunner.class);

    private static final String SEEDED_PLACEHOLDER = "$2a$10$PH.aHGFakeBcryptHashAdminOneXXXXXXXXXXXXXXXXXXXXXXX";
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$");

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;
    private final String superAdminEmail;

    public AdminPasswordBootstrapRunner(AdminRepository adminRepository,
                                        PasswordEncoder passwordEncoder,
                                        @Value("${app.security.initial-super-admin-password:}") String initialPassword,
                                        @Value("${app.security.super-admin-email:priya.sharma@sewngo.org}") String superAdminEmail) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
        this.superAdminEmail = superAdminEmail;
    }

    @Override
    public void run(String... args) {
        bootstrapSuperAdminPassword();
    }

    public void bootstrapSuperAdminPassword() {
        Optional<Admin> adminOpt = adminRepository.findByEmail(superAdminEmail);
        if (adminOpt.isEmpty()) {
            log.info("No seeded SUPER_ADMIN account found with email '{}'. Skipping password bootstrap.", superAdminEmail);
            return;
        }

        Admin admin = adminOpt.get();

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new IllegalStateException("Seeded SUPER_ADMIN account is inactive. Application startup aborted.");
        }

        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new IllegalStateException("Account found with email '" + superAdminEmail + "' does not have SUPER_ADMIN role. Application startup aborted.");
        }

        String currentHash = admin.getPasswordHash();

        if (currentHash != null && BCRYPT_PATTERN.matcher(currentHash).matches() && !isPlaceholder(currentHash)) {
            log.info("SUPER_ADMIN account already has a valid BCrypt password hash. Skipping bootstrap.");
            return;
        }

        if (isPlaceholder(currentHash)) {
            if (initialPassword == null || initialPassword.isBlank()) {
                throw new IllegalStateException(
                        "Seeded SUPER_ADMIN account has placeholder hash, but app.security.initial-super-admin-password is null or blank. Application startup aborted."
                );
            }

            // Encode untrimmed password
            admin.setPasswordHash(passwordEncoder.encode(initialPassword));
            adminRepository.save(admin);
            log.info("Successfully bootstrapped initial SUPER_ADMIN password.");
            return;
        }

        throw new IllegalStateException(
                "SUPER_ADMIN password hash in database is corrupted or in an unsupported format. Application startup aborted."
        );
    }

    private boolean isPlaceholder(String hash) {
        return hash != null && (hash.equals(SEEDED_PLACEHOLDER) || hash.contains("FakeBcryptHash"));
    }
}
