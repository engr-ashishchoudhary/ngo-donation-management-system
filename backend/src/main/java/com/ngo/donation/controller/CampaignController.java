package com.ngo.donation.controller;

import com.ngo.donation.dto.request.CampaignRequest;
import com.ngo.donation.dto.response.CampaignFinancialsResponse;
import com.ngo.donation.dto.response.CampaignResponse;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    @Autowired
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public ResponseEntity<List<CampaignFinancialsResponse>> getAllCampaigns(
            @RequestParam(name = "status", required = false) CampaignStatus status) {
        List<CampaignFinancialsResponse> campaigns = campaignService.getAllCampaignFinancials(status);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignFinancialsResponse> getCampaignById(@PathVariable("id") Integer id) {
        CampaignFinancialsResponse campaign = campaignService.getCampaignFinancialsById(id);
        return ResponseEntity.ok(campaign);
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer adminId = principal != null ? principal.getId() : null;
        CampaignResponse response = campaignService.createCampaign(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable("id") Integer id,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<CampaignResponse> closeCampaign(@PathVariable("id") Integer id) {
        CampaignResponse response = campaignService.closeCampaign(id);
        return ResponseEntity.ok(response);
    }
}
