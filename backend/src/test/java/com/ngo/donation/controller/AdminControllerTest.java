package com.ngo.donation.controller;

import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.DashboardResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/admins - Success returns 200 OK with List<AdminResponse>")
    void testGetAllAdmins_Success() throws Exception {
        AdminResponse admin1 = new AdminResponse(1, "Priya Sharma", "priya@sewngo.org", AdminRole.SUPER_ADMIN, true, LocalDateTime.now());
        AdminResponse admin2 = new AdminResponse(2, "Rajan Mehta", "rajan@sewngo.org", AdminRole.ADMIN, true, LocalDateTime.now());
        when(adminService.getAllAdmins()).thenReturn(List.of(admin1, admin2));

        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminId").value(1))
                .andExpect(jsonPath("$[0].role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[1].adminId").value(2))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));

        verify(adminService).getAllAdmins();
    }

    @Test
    @DisplayName("GET /api/admin/donors - Success returns 200 OK with List<DonorResponse>")
    void testGetAllDonors_Success() throws Exception {
        DonorResponse donor = new DonorResponse(5, "Rahul Verma", "rahul@email.com", "9876543210", "Bengaluru", true, LocalDateTime.now());
        when(adminService.getAllDonors()).thenReturn(List.of(donor));

        mockMvc.perform(get("/api/admin/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donorId").value(5))
                .andExpect(jsonPath("$[0].name").value("Rahul Verma"))
                .andExpect(jsonPath("$[0].email").value("rahul@email.com"));

        verify(adminService).getAllDonors();
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - Success returns 200 OK with DashboardResponse")
    void testGetDashboardSummary_Success() throws Exception {
        DashboardResponse mockDashboard = new DashboardResponse(
                2L, 42L, 5L, 3L,
                new BigDecimal("1250000.00"), new BigDecimal("400000.00"), new BigDecimal("850000.00"),
                100L, 5L, 2L, 1L
        );
        when(adminService.getDashboardSummary()).thenReturn(mockDashboard);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveDonors").value(42))
                .andExpect(jsonPath("$.activeCampaignsCount").value(3))
                .andExpect(jsonPath("$.grandTotalCollected").value(1250000.00))
                .andExpect(jsonPath("$.grandTotalSpent").value(400000.00));

        verify(adminService).getDashboardSummary();
    }
}
