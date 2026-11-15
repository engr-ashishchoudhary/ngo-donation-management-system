package com.ngo.donation.controller;

import com.ngo.donation.dto.request.ExpenseRequest;
import com.ngo.donation.dto.response.ExpenseDetailsResponse;
import com.ngo.donation.dto.response.ExpenseResponse;
import com.ngo.donation.entity.enums.ExpenseCategory;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.exception.InsufficientFundsException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.ExpenseService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(expenseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/campaigns/{campaignId}/expenses - Success returns 200 OK with List<ExpenseDetailsResponse>")
    void testGetExpensesByCampaign_Success() throws Exception {
        ExpenseDetailsResponse expense = new ExpenseDetailsResponse(
                3, 1, "Clean Water", 1, "Priya Sharma",
                "Water purifier units", new BigDecimal("30000.00"), ExpenseCategory.SUPPLIES,
                LocalDate.of(2026, 11, 10), LocalDateTime.now()
        );
        when(expenseService.getExpensesByCampaign(1)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/campaigns/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expenseId").value(3))
                .andExpect(jsonPath("$[0].campaignId").value(1))
                .andExpect(jsonPath("$[0].amount").value(30000.00))
                .andExpect(jsonPath("$[0].category").value("SUPPLIES"));

        verify(expenseService).getExpensesByCampaign(1);
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/expenses - Success returns 201 Created and forwards admin ID")
    void testRecordExpense_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(2, "admin@ngo.org", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        ExpenseResponse mockResponse = new ExpenseResponse(
                7, 1, new BigDecimal("8000.00"), ExpenseCategory.LOGISTICS,
                "Diesel for water trucks", LocalDate.of(2026, 11, 14), LocalDateTime.now()
        );
        when(expenseService.recordExpense(eq(1), any(ExpenseRequest.class), eq(2))).thenReturn(mockResponse);

        String json = """
                {
                  "description": "Diesel for water trucks",
                  "amount": 8000.00,
                  "category": "LOGISTICS",
                  "expenseDate": "2026-11-14"
                }
                """;

        mockMvc.perform(post("/api/campaigns/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseId").value(7))
                .andExpect(jsonPath("$.campaignId").value(1))
                .andExpect(jsonPath("$.amount").value(8000.00))
                .andExpect(jsonPath("$.category").value("LOGISTICS"));

        verify(expenseService).recordExpense(eq(1), any(ExpenseRequest.class), eq(2));
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/expenses - Validation failure returns 400 Bad Request")
    void testRecordExpense_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "description": "",
                  "amount": -50.00
                }
                """;

        mockMvc.perform(post("/api/campaigns/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(expenseService, never()).recordExpense(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/expenses - InsufficientFundsException returns 422 Unprocessable Entity")
    void testRecordExpense_InsufficientFunds() throws Exception {
        UserPrincipal principal = new UserPrincipal(1, "superadmin@ngo.org", "ROLE_SUPER_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(expenseService.recordExpense(eq(1), any(ExpenseRequest.class), eq(1)))
                .thenThrow(new InsufficientFundsException("ERROR: Expense exceeds available campaign funds."));

        String json = """
                {
                  "description": "Exorbitant equipment purchase",
                  "amount": 999999.00,
                  "category": "SUPPLIES",
                  "expenseDate": "2026-11-14"
                }
                """;

        mockMvc.perform(post("/api/campaigns/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("ERROR: Expense exceeds available campaign funds."));

        verify(expenseService).recordExpense(eq(1), any(ExpenseRequest.class), eq(1));
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/expenses - ResourceNotFoundException returns 404 Not Found")
    void testRecordExpense_CampaignNotFound() throws Exception {
        UserPrincipal principal = new UserPrincipal(1, "superadmin@ngo.org", "ROLE_SUPER_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(expenseService.recordExpense(eq(99), any(ExpenseRequest.class), eq(1)))
                .thenThrow(new ResourceNotFoundException("ERROR: Campaign with ID 99 not found."));

        String json = """
                {
                  "description": "Supplies",
                  "amount": 500.00,
                  "category": "SUPPLIES",
                  "expenseDate": "2026-11-14"
                }
                """;

        mockMvc.perform(post("/api/campaigns/99/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("ERROR: Campaign with ID 99 not found."));

        verify(expenseService).recordExpense(eq(99), any(ExpenseRequest.class), eq(1));
    }
}
