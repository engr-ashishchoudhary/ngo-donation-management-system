package com.ngo.donation.service.impl;

import com.ngo.donation.dto.request.AdminLoginRequest;
import com.ngo.donation.dto.request.AdminRegisterRequest;
import com.ngo.donation.dto.request.DonorLoginRequest;
import com.ngo.donation.dto.request.DonorRegisterRequest;
import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.AuthResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.exception.ConflictException;
import com.ngo.donation.exception.ForbiddenException;
import com.ngo.donation.exception.UnauthorizedException;
import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.AuthService;
import com.ngo.donation.service.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AdminRepository adminRepository;
    private final DonorRepository donorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthServiceImpl(AdminRepository adminRepository,
                           DonorRepository donorRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.adminRepository = adminRepository;
        this.donorRepository = donorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AdminResponse registerAdmin(AdminRegisterRequest request, Integer authenticatedAdminId) {
        if (authenticatedAdminId == null) {
            throw new ForbiddenException("Authentication required: Only SUPER_ADMIN can register new administrators.");
        }

        Admin requestingAdmin = adminRepository.findById(authenticatedAdminId)
                .orElseThrow(() -> new ForbiddenException("Requesting administrator not found."));

        if (requestingAdmin.getRole() != AdminRole.SUPER_ADMIN || !Boolean.TRUE.equals(requestingAdmin.getIsActive())) {
            throw new ForbiddenException("Only active SUPER_ADMIN can register new administrators.");
        }

        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        AdminRole targetRole = request.getRole() != null ? request.getRole() : AdminRole.ADMIN;
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Admin admin = new Admin(request.getName(), request.getEmail(), encodedPassword, targetRole);
        Admin savedAdmin = adminRepository.save(admin);

        return new AdminResponse(
                savedAdmin.getAdminId(),
                savedAdmin.getName(),
                savedAdmin.getEmail(),
                savedAdmin.getRole(),
                savedAdmin.getIsActive(),
                savedAdmin.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse loginAdmin(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new UnauthorizedException("Admin account is deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String roleClaim = "ROLE_" + admin.getRole().name();
        String token = jwtTokenProvider.generateToken(admin.getAdminId(), admin.getEmail(), roleClaim);

        return new AuthResponse(
                token,
                admin.getAdminId(),
                admin.getName(),
                admin.getEmail(),
                admin.getRole().name()
        );
    }

    @Override
    @Transactional
    public DonorResponse registerDonor(DonorRegisterRequest request) {
        if (donorRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Donor donor = new Donor(
                request.getName(),
                request.getEmail(),
                encodedPassword,
                request.getPhone(),
                request.getAddress()
        );
        Donor savedDonor = donorRepository.save(donor);

        return new DonorResponse(
                savedDonor.getDonorId(),
                savedDonor.getName(),
                savedDonor.getEmail(),
                savedDonor.getPhone(),
                savedDonor.getAddress(),
                savedDonor.getIsActive(),
                savedDonor.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse loginDonor(DonorLoginRequest request) {
        Donor donor = donorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!Boolean.TRUE.equals(donor.getIsActive())) {
            throw new UnauthorizedException("Donor account is deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), donor.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = jwtTokenProvider.generateToken(donor.getDonorId(), donor.getEmail(), "ROLE_DONOR");

        return new AuthResponse(
                token,
                donor.getDonorId(),
                donor.getName(),
                donor.getEmail(),
                "DONOR"
        );
    }
}
