package com.ngo.donation.controller;

import com.ngo.donation.dto.request.DonationRequest;
import com.ngo.donation.dto.request.DonationStatusUpdateRequest;
import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.dto.response.DonationResponse;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    @Autowired
    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping
    public ResponseEntity<List<DonationDetailsResponse>> getAllDonations(
            @RequestParam(name = "campaignId", required = false) Integer campaignId,
            @RequestParam(name = "status", required = false) DonationStatus status) {
        List<DonationDetailsResponse> donations = donationService.getAllDonationDetails(campaignId, status);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DonationDetailsResponse>> getMyDonations(
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer donorId = principal != null ? principal.getId() : null;
        List<DonationDetailsResponse> donations = donationService.getMyDonations(donorId);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationDetailsResponse> getDonationById(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer userId = principal != null ? principal.getId() : null;
        String role = principal != null ? principal.getRole() : null;
        DonationDetailsResponse donation = donationService.getDonationDetailsById(id, userId, role);
        return ResponseEntity.ok(donation);
    }

    @PostMapping
    public ResponseEntity<DonationResponse> recordDonation(
            @Valid @RequestBody DonationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer donorId = principal != null ? principal.getId() : null;
        DonationResponse response = donationService.recordDonation(request, donorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DonationDetailsResponse> updateDonationStatus(
            @PathVariable("id") Integer id,
            @Valid @RequestBody DonationStatusUpdateRequest request) {
        DonationDetailsResponse response = donationService.updateDonationStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
