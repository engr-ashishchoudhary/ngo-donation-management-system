package com.ngo.donation.controller;

import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.DashboardResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminResponse>> getAllAdmins() {
        List<AdminResponse> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/donors")
    public ResponseEntity<List<DonorResponse>> getAllDonors() {
        List<DonorResponse> donors = adminService.getAllDonors();
        return ResponseEntity.ok(donors);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardSummary() {
        DashboardResponse dashboard = adminService.getDashboardSummary();
        return ResponseEntity.ok(dashboard);
    }
}
