package com.ngo.donation.service;

import com.ngo.donation.dto.request.DonationRequest;
import com.ngo.donation.dto.request.DonationStatusUpdateRequest;
import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.dto.response.DonationResponse;
import com.ngo.donation.entity.enums.DonationStatus;

import java.util.List;

public interface DonationService {

    DonationResponse recordDonation(DonationRequest request, Integer donorId);

    List<DonationDetailsResponse> getAllDonationDetails(Integer campaignId, DonationStatus status);

    List<DonationDetailsResponse> getMyDonations(Integer donorId);

    DonationDetailsResponse getDonationDetailsById(Integer donationId, Integer authenticatedUserId, String userRole);

    DonationDetailsResponse updateDonationStatus(Integer donationId, DonationStatusUpdateRequest request);
}
