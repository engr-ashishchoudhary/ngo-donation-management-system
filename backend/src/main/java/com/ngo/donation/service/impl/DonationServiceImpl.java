package com.ngo.donation.service.impl;

import com.ngo.donation.dto.request.DonationRequest;
import com.ngo.donation.dto.request.DonationStatusUpdateRequest;
import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.dto.response.DonationResponse;
import com.ngo.donation.entity.Campaign;
import com.ngo.donation.entity.Donation;
import com.ngo.donation.entity.Donor;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.exception.ConflictException;
import com.ngo.donation.exception.ForbiddenException;
import com.ngo.donation.exception.InvalidOperationException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.mapper.DonationDetailsRowMapper;
import com.ngo.donation.repository.CampaignRepository;
import com.ngo.donation.repository.DonationRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final DonorRepository donorRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DonationServiceImpl(DonationRepository donationRepository,
                               CampaignRepository campaignRepository,
                               DonorRepository donorRepository,
                               JdbcTemplate jdbcTemplate) {
        this.donationRepository = donationRepository;
        this.campaignRepository = campaignRepository;
        this.donorRepository = donorRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public DonationResponse recordDonation(DonationRequest request, Integer donorId) {
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + request.getCampaignId()));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new InvalidOperationException("Donations can only be made to ACTIVE campaigns. Current status: " + campaign.getStatus());
        }

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with ID: " + donorId));

        if (!Boolean.TRUE.equals(donor.getIsActive())) {
            throw new InvalidOperationException("Donor account is inactive.");
        }

        if (request.getTransactionRef() != null && !request.getTransactionRef().trim().isEmpty()) {
            if (donationRepository.existsByTransactionRef(request.getTransactionRef().trim())) {
                throw new ConflictException("Transaction reference already exists: " + request.getTransactionRef());
            }
        }

        Donation donation = new Donation(
                donor,
                campaign,
                request.getAmount(),
                request.getPaymentMethod(),
                DonationStatus.PENDING,
                request.getTransactionRef() != null ? request.getTransactionRef().trim() : null,
                request.getNotes()
        );

        Donation saved = donationRepository.save(donation);

        return new DonationResponse(
                saved.getDonationId(),
                campaign.getCampaignId(),
                saved.getAmount(),
                saved.getPaymentMethod(),
                saved.getStatus(),
                saved.getTransactionRef(),
                saved.getDonatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationDetailsResponse> getAllDonationDetails(Integer campaignId, DonationStatus status) {
        String sql = "SELECT * FROM vw_donation_details WHERE (? IS NULL OR campaign_id = ?) AND (? IS NULL OR status = ?) ORDER BY donated_at DESC";
        String statusParam = status != null ? status.name() : null;
        return jdbcTemplate.query(sql, new DonationDetailsRowMapper(), campaignId, campaignId, statusParam, statusParam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationDetailsResponse> getMyDonations(Integer donorId) {
        String sql = "SELECT * FROM vw_donation_details WHERE donor_id = ? ORDER BY donated_at DESC";
        return jdbcTemplate.query(sql, new DonationDetailsRowMapper(), donorId);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationDetailsResponse getDonationDetailsById(Integer donationId, Integer authenticatedUserId, String userRole) {
        String sql = "SELECT * FROM vw_donation_details WHERE donation_id = ?";
        List<DonationDetailsResponse> results = jdbcTemplate.query(sql, new DonationDetailsRowMapper(), donationId);

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Donation not found with ID: " + donationId);
        }

        DonationDetailsResponse details = results.get(0);

        if (userRole != null) {
            String roleUpper = userRole.toUpperCase();
            if (roleUpper.equals("ROLE_DONOR") || roleUpper.equals("DONOR")) {
                if (authenticatedUserId == null || !details.getDonorId().equals(authenticatedUserId)) {
                    throw new ForbiddenException("Donors can only access their own donation details.");
                }
            } else if (!roleUpper.equals("ROLE_ADMIN") && !roleUpper.equals("ADMIN")
                    && !roleUpper.equals("ROLE_SUPER_ADMIN") && !roleUpper.equals("SUPER_ADMIN")
                    && !roleUpper.equals("ROLE_CAMPAIGN_MANAGER") && !roleUpper.equals("CAMPAIGN_MANAGER")
                    && !roleUpper.equals("ROLE_FINANCIAL_OFFICER") && !roleUpper.equals("FINANCIAL_OFFICER")) {
                throw new ForbiddenException("Access denied: Insufficient permissions.");
            }
        }

        return details;
    }

    @Override
    @Transactional
    public DonationDetailsResponse updateDonationStatus(Integer donationId, DonationStatusUpdateRequest request) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with ID: " + donationId));

        DonationStatus currentStatus = donation.getStatus();
        DonationStatus newStatus = request.getStatus();

        boolean isValidTransition = (currentStatus == DonationStatus.PENDING && (newStatus == DonationStatus.COMPLETED || newStatus == DonationStatus.FAILED))
                || (currentStatus == DonationStatus.COMPLETED && newStatus == DonationStatus.REFUNDED);

        if (!isValidTransition) {
            throw new InvalidOperationException("Invalid donation status transition from " + currentStatus + " to " + newStatus);
        }

        donation.setStatus(newStatus);

        try {
            donationRepository.saveAndFlush(donation);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Invalid donation status transition")) {
                throw new InvalidOperationException("Invalid donation status transition: current donation status is no longer eligible.", ex);
            }
            throw ex;
        }

        return getDonationDetailsById(donationId, null, null);
    }
}
