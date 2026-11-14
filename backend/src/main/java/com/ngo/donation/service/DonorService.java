package com.ngo.donation.service;

import com.ngo.donation.dto.request.DonorProfileUpdateRequest;
import com.ngo.donation.dto.response.DonorProfileResponse;

public interface DonorService {

    DonorProfileResponse getDonorProfile(Integer donorId);

    DonorProfileResponse updateDonorProfile(Integer donorId, DonorProfileUpdateRequest request);
}
