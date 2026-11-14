package com.ngo.donation.service;

import com.ngo.donation.dto.request.AdminLoginRequest;
import com.ngo.donation.dto.request.AdminRegisterRequest;
import com.ngo.donation.dto.request.DonorLoginRequest;
import com.ngo.donation.dto.request.DonorRegisterRequest;
import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.AuthResponse;
import com.ngo.donation.dto.response.DonorResponse;

public interface AuthService {

    AdminResponse registerAdmin(AdminRegisterRequest request, Integer authenticatedAdminId);

    AuthResponse loginAdmin(AdminLoginRequest request);

    DonorResponse registerDonor(DonorRegisterRequest request);

    AuthResponse loginDonor(DonorLoginRequest request);
}
