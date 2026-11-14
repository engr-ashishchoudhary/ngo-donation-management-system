package com.ngo.donation.service;

import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.DashboardResponse;
import com.ngo.donation.dto.response.DonorResponse;

import java.util.List;

public interface AdminService {

    List<AdminResponse> getAllAdmins();

    List<DonorResponse> getAllDonors();

    DashboardResponse getDashboardSummary();
}
