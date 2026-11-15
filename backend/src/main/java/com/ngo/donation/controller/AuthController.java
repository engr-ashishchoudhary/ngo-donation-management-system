package com.ngo.donation.controller;

import com.ngo.donation.dto.request.AdminLoginRequest;
import com.ngo.donation.dto.request.AdminRegisterRequest;
import com.ngo.donation.dto.request.DonorLoginRequest;
import com.ngo.donation.dto.request.DonorRegisterRequest;
import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.AuthResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AdminResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        Integer adminId = principal != null ? principal.getId() : null;
        AdminResponse response = authService.registerAdmin(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        AuthResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/donor/register")
    public ResponseEntity<DonorResponse> registerDonor(@Valid @RequestBody DonorRegisterRequest request) {
        DonorResponse response = authService.registerDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/donor/login")
    public ResponseEntity<AuthResponse> loginDonor(@Valid @RequestBody DonorLoginRequest request) {
        AuthResponse response = authService.loginDonor(request);
        return ResponseEntity.ok(response);
    }
}
