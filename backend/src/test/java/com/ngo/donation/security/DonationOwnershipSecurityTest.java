package com.ngo.donation.security;

import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;
import com.ngo.donation.exception.ForbiddenException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.mapper.DonationDetailsRowMapper;
import com.ngo.donation.repository.CampaignRepository;
import com.ngo.donation.repository.DonationRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.impl.DonationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationOwnershipSecurityTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DonationServiceImpl donationService;

    private DonationDetailsResponse sampleDonation;

    @BeforeEach
    void setUp() {
        // Correct constructor order: (donationRepository, campaignRepository, donorRepository, jdbcTemplate)
        donationService = new DonationServiceImpl(donationRepository, campaignRepository, donorRepository, jdbcTemplate);

        // 12-arg constructor: (donationId, donorId, donorName, donorEmail, campaignId, campaignName,
        //                       amount, paymentMethod, status, transactionRef, notes, donatedAt)
        sampleDonation = new DonationDetailsResponse(
                100,                        // donationId
                10,                         // donorId
                "Rahul Verma",              // donorName
                "rahul@ngo.org",            // donorEmail
                5,                          // campaignId
                "Education Campaign",       // campaignName
                new BigDecimal("500.00"),   // amount
                PaymentMethod.UPI,          // paymentMethod
                DonationStatus.COMPLETED,   // status
                "TXN12345",                 // transactionRef
                "Keep it up",               // notes
                LocalDateTime.now()         // donatedAt
        );
    }

    @Test
    @DisplayName("Donor can view their own donation receipt")
    void testDonorCanAccessOwnDonation() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        DonationDetailsResponse result = donationService.getDonationDetailsById(100, 10, "ROLE_DONOR");
        assertNotNull(result);
        assertEquals(100, result.getDonationId());
        assertEquals(10, result.getDonorId());
    }

    @Test
    @DisplayName("Donor cannot view another donor's donation receipt (throws ForbiddenException)")
    void testDonorCannotAccessOtherDonorDonation() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        // authenticatedUserId = 99, but donation donorId = 10
        assertThrows(ForbiddenException.class, () ->
                donationService.getDonationDetailsById(100, 99, "ROLE_DONOR")
        );
    }

    @Test
    @DisplayName("Donor with null authenticatedUserId throws ForbiddenException")
    void testDonorWithNullUserIdThrowsForbidden() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        assertThrows(ForbiddenException.class, () ->
                donationService.getDonationDetailsById(100, null, "ROLE_DONOR")
        );
    }

    @Test
    @DisplayName("Administrator (SUPER_ADMIN) can view any donor's donation details")
    void testSuperAdminCanAccessAnyDonation() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        DonationDetailsResponse result = donationService.getDonationDetailsById(100, 1, "ROLE_SUPER_ADMIN");
        assertNotNull(result);
        assertEquals(100, result.getDonationId());
    }

    @Test
    @DisplayName("Administrator (ADMIN) can view any donor's donation details")
    void testAdminCanAccessAnyDonation() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        DonationDetailsResponse result = donationService.getDonationDetailsById(100, 2, "ROLE_ADMIN");
        assertNotNull(result);
        assertEquals(100, result.getDonationId());
    }

    @Test
    @DisplayName("Unknown or unsupported role throws ForbiddenException")
    void testUnknownRoleThrowsForbidden() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(100)))
                .thenReturn(List.of(sampleDonation));

        assertThrows(ForbiddenException.class, () ->
                donationService.getDonationDetailsById(100, 10, "ROLE_GUEST")
        );
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when donation does not exist")
    void testDonationNotFoundThrowsException() {
        when(jdbcTemplate.query(any(String.class), any(DonationDetailsRowMapper.class), eq(999)))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () ->
                donationService.getDonationDetailsById(999, 10, "ROLE_DONOR")
        );
    }
}
