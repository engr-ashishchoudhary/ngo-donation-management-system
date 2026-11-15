package com.ngo.donation.controller;

import com.ngo.donation.dto.request.DonationRequest;
import com.ngo.donation.dto.request.DonationStatusUpdateRequest;
import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.dto.response.DonationResponse;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;
import com.ngo.donation.exception.ForbiddenException;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.exception.InvalidOperationException;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DonationControllerTest {

    @Mock
    private DonationService donationService;

    @InjectMocks
    private DonationController donationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(donationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/donations - Without filters calls getAllDonationDetails(null, null)")
    void testGetAllDonations_NoFilter() throws Exception {
        when(donationService.getAllDonationDetails(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/donations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(donationService).getAllDonationDetails(null, null);
    }

    @Test
    @DisplayName("GET /api/donations?campaignId=1&status=COMPLETED - With filters calls service with arguments")
    void testGetAllDonations_WithFilters() throws Exception {
        when(donationService.getAllDonationDetails(1, DonationStatus.COMPLETED)).thenReturn(List.of());

        mockMvc.perform(get("/api/donations")
                        .param("campaignId", "1")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(donationService).getAllDonationDetails(1, DonationStatus.COMPLETED);
    }

    @Test
    @DisplayName("GET /api/donations/my - Extracts donorId from UserPrincipal and returns donations")
    void testGetMyDonations_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        DonationDetailsResponse donation = new DonationDetailsResponse(
                10, 5, "Rahul Verma", "rahul@email.com", 1, "Clean Water",
                new BigDecimal("5000.00"), PaymentMethod.UPI, DonationStatus.COMPLETED,
                "UPI12345", "Notes", LocalDateTime.now()
        );
        when(donationService.getMyDonations(5)).thenReturn(List.of(donation));

        mockMvc.perform(get("/api/donations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donationId").value(10))
                .andExpect(jsonPath("$[0].donorId").value(5))
                .andExpect(jsonPath("$[0].amount").value(5000.00));

        verify(donationService).getMyDonations(5);
    }

    @Test
    @DisplayName("GET /api/donations/{id} - Forwards both principal ID and role for ownership check")
    void testGetDonationById_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        DonationDetailsResponse donation = new DonationDetailsResponse(
                10, 5, "Rahul Verma", "rahul@email.com", 1, "Clean Water",
                new BigDecimal("5000.00"), PaymentMethod.UPI, DonationStatus.COMPLETED,
                "UPI12345", "Notes", LocalDateTime.now()
        );
        when(donationService.getDonationDetailsById(10, 5, "ROLE_DONOR")).thenReturn(donation);

        mockMvc.perform(get("/api/donations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donationId").value(10))
                .andExpect(jsonPath("$.donorId").value(5));

        verify(donationService).getDonationDetailsById(10, 5, "ROLE_DONOR");
    }

    @Test
    @DisplayName("GET /api/donations/{id} - ForbiddenException returns 403 Forbidden")
    void testGetDonationById_ForbiddenOwnership() throws Exception {
        UserPrincipal principal = new UserPrincipal(8, "other@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(donationService.getDonationDetailsById(10, 8, "ROLE_DONOR"))
                .thenThrow(new ForbiddenException("Donors can only access their own donation details."));

        mockMvc.perform(get("/api/donations/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Donors can only access their own donation details."));

        verify(donationService).getDonationDetailsById(10, 8, "ROLE_DONOR");
    }

    @Test
    @DisplayName("POST /api/donations - Success returns 201 Created and forwards donor ID")
    void testRecordDonation_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        DonationResponse mockResponse = new DonationResponse(
                15, 1, new BigDecimal("1000.00"), PaymentMethod.UPI, DonationStatus.PENDING,
                "UPI20261114099", LocalDateTime.now()
        );
        when(donationService.recordDonation(any(DonationRequest.class), eq(5))).thenReturn(mockResponse);

        String json = """
                {
                  "campaignId": 1,
                  "amount": 1000.00,
                  "paymentMethod": "UPI",
                  "transactionRef": "UPI20261114099",
                  "notes": "Happy to help!"
                }
                """;

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donationId").value(15))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(donationService).recordDonation(any(DonationRequest.class), eq(5));
    }

    @Test
    @DisplayName("POST /api/donations - Validation failure returns 400 Bad Request")
    void testRecordDonation_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "amount": -10.00
                }
                """;

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(donationService, never()).recordDonation(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/donations/{id}/status - Success returns 200 OK")
    void testUpdateDonationStatus_Success() throws Exception {
        DonationDetailsResponse updated = new DonationDetailsResponse(
                10, 5, "Rahul Verma", "rahul@email.com", 1, "Clean Water",
                new BigDecimal("5000.00"), PaymentMethod.UPI, DonationStatus.COMPLETED,
                "UPI12345", "Notes", LocalDateTime.now()
        );
        when(donationService.updateDonationStatus(eq(10), any(DonationStatusUpdateRequest.class)))
                .thenReturn(updated);

        String json = """
                {
                  "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/donations/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donationId").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(donationService).updateDonationStatus(eq(10), any(DonationStatusUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/donations/{id}/status - InvalidOperationException returns 422 Unprocessable Entity")
    void testUpdateDonationStatus_InvalidTransition() throws Exception {
        when(donationService.updateDonationStatus(eq(10), any(DonationStatusUpdateRequest.class)))
                .thenThrow(new InvalidOperationException("Invalid donation status transition from FAILED to COMPLETED"));

        String json = """
                {
                  "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/donations/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Invalid donation status transition from FAILED to COMPLETED"));

        verify(donationService).updateDonationStatus(eq(10), any(DonationStatusUpdateRequest.class));
    }
}
