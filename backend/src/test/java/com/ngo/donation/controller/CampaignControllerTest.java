package com.ngo.donation.controller;

import com.ngo.donation.dto.request.CampaignRequest;
import com.ngo.donation.dto.response.CampaignFinancialsResponse;
import com.ngo.donation.dto.response.CampaignResponse;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.exception.InvalidOperationException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.CampaignService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(campaignController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/campaigns - Without status filter calls getAllCampaignFinancials(null)")
    void testGetAllCampaigns_NoFilter() throws Exception {
        CampaignFinancialsResponse c1 = new CampaignFinancialsResponse(
                1, "Clean Water", "Clean water project", new BigDecimal("500000.00"),
                new BigDecimal("200000.00"), new BigDecimal("50000.00"), new BigDecimal("150000.00"),
                CampaignStatus.ACTIVE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDateTime.now()
        );
        when(campaignService.getAllCampaignFinancials(null)).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].campaignId").value(1))
                .andExpect(jsonPath("$[0].name").value("Clean Water"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].availableFunds").value(150000.00));

        verify(campaignService).getAllCampaignFinancials(null);
    }

    @Test
    @DisplayName("GET /api/campaigns?status=ACTIVE - With status filter calls getAllCampaignFinancials(ACTIVE)")
    void testGetAllCampaigns_WithStatusFilter() throws Exception {
        when(campaignService.getAllCampaignFinancials(CampaignStatus.ACTIVE)).thenReturn(List.of());

        mockMvc.perform(get("/api/campaigns").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(campaignService).getAllCampaignFinancials(CampaignStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /api/campaigns/{id} - Success returns 200 OK with CampaignFinancialsResponse")
    void testGetCampaignById_Success() throws Exception {
        CampaignFinancialsResponse mockResponse = new CampaignFinancialsResponse(
                1, "Clean Water", "Description", new BigDecimal("500000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                CampaignStatus.ACTIVE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDateTime.now()
        );
        when(campaignService.getCampaignFinancialsById(1)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(1))
                .andExpect(jsonPath("$.name").value("Clean Water"));

        verify(campaignService).getCampaignFinancialsById(1);
    }

    @Test
    @DisplayName("GET /api/campaigns/{id} - Not found returns 404 Not Found")
    void testGetCampaignById_NotFound() throws Exception {
        when(campaignService.getCampaignFinancialsById(99))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 99"));

        mockMvc.perform(get("/api/campaigns/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Campaign not found with ID: 99"));

        verify(campaignService).getCampaignFinancialsById(99);
    }

    @Test
    @DisplayName("POST /api/campaigns - Success returns 201 Created and forwards creator admin ID")
    void testCreateCampaign_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(2, "admin@ngo.org", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        CampaignResponse mockResponse = new CampaignResponse(
                5, "Education Drive", "Books for kids", new BigDecimal("100000.00"),
                CampaignStatus.ACTIVE, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 1), LocalDateTime.now()
        );
        when(campaignService.createCampaign(any(CampaignRequest.class), eq(2))).thenReturn(mockResponse);

        String json = """
                {
                  "name": "Education Drive",
                  "description": "Books for kids",
                  "goalAmount": 100000.00,
                  "startDate": "2026-06-01",
                  "endDate": "2026-12-01"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(5))
                .andExpect(jsonPath("$.name").value("Education Drive"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(campaignService).createCampaign(any(CampaignRequest.class), eq(2));
    }

    @Test
    @DisplayName("POST /api/campaigns - Validation failure returns 400 Bad Request")
    void testCreateCampaign_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "goalAmount": -50.00,
                  "startDate": "2026-12-01",
                  "endDate": "2026-06-01"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(campaignService, never()).createCampaign(any(), any());
    }

    @Test
    @DisplayName("PUT /api/campaigns/{id} - Success returns 200 OK")
    void testUpdateCampaign_Success() throws Exception {
        CampaignResponse mockResponse = new CampaignResponse(
                1, "Updated Name", "Updated Desc", new BigDecimal("250000.00"),
                CampaignStatus.ACTIVE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDateTime.now()
        );
        when(campaignService.updateCampaign(eq(1), any(CampaignRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                  "name": "Updated Name",
                  "description": "Updated Desc",
                  "goalAmount": 250000.00,
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31"
                }
                """;

        mockMvc.perform(put("/api/campaigns/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"));

        verify(campaignService).updateCampaign(eq(1), any(CampaignRequest.class));
    }

    @Test
    @DisplayName("PUT /api/campaigns/{id} - InvalidOperationException returns 422 Unprocessable Entity")
    void testUpdateCampaign_InvalidOperation() throws Exception {
        when(campaignService.updateCampaign(eq(1), any(CampaignRequest.class)))
                .thenThrow(new InvalidOperationException("Cannot update campaign in terminal status: CLOSED"));

        String json = """
                {
                  "name": "Updated Name",
                  "description": "Updated Desc",
                  "goalAmount": 250000.00,
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31"
                }
                """;

        mockMvc.perform(put("/api/campaigns/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Cannot update campaign in terminal status: CLOSED"));

        verify(campaignService).updateCampaign(eq(1), any(CampaignRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id}/close - Success returns 200 OK")
    void testCloseCampaign_Success() throws Exception {
        CampaignResponse mockResponse = new CampaignResponse(
                1, "Clean Water", "Desc", new BigDecimal("500000.00"),
                CampaignStatus.CLOSED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDateTime.now()
        );
        when(campaignService.closeCampaign(1)).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/campaigns/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(1))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(campaignService).closeCampaign(1);
    }
}
