package com.ngo.donation.controller;

import com.ngo.donation.dto.request.DonorProfileUpdateRequest;
import com.ngo.donation.dto.response.DonorProfileResponse;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donor")
public class DonorController {

    private final DonorService donorService;

    @Autowired
    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @GetMapping("/profile")
    public ResponseEntity<DonorProfileResponse> getDonorProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer donorId = principal != null ? principal.getId() : null;
        DonorProfileResponse profile = donorService.getDonorProfile(donorId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<DonorProfileResponse> updateDonorProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DonorProfileUpdateRequest request) {
        Integer donorId = principal != null ? principal.getId() : null;
        DonorProfileResponse profile = donorService.updateDonorProfile(donorId, request);
        return ResponseEntity.ok(profile);
    }
}
