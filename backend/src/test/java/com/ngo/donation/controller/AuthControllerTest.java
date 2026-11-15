package com.ngo.donation.controller;

import com.ngo.donation.dto.request.AdminLoginRequest;
import com.ngo.donation.dto.request.AdminRegisterRequest;
import com.ngo.donation.dto.request.DonorLoginRequest;
import com.ngo.donation.dto.request.DonorRegisterRequest;
import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.AuthResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.AuthService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/admin/register - Success returns 201 Created and forwards principal ID")
    void testAdminRegister_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(1, "superadmin@ngo.org", "ROLE_SUPER_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        AdminResponse mockResponse = new AdminResponse(
                10, "Staff Admin", "staff@ngo.org", AdminRole.ADMIN, true, LocalDateTime.now()
        );
        when(authService.registerAdmin(any(AdminRegisterRequest.class), eq(1))).thenReturn(mockResponse);

        String json = """
                {
                  "name": "Staff Admin",
                  "email": "staff@ngo.org",
                  "password": "ValidPassword123!",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adminId").value(10))
                .andExpect(jsonPath("$.name").value("Staff Admin"))
                .andExpect(jsonPath("$.email").value("staff@ngo.org"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(authService).registerAdmin(any(AdminRegisterRequest.class), eq(1));
    }

    @Test
    @DisplayName("POST /api/auth/admin/register - Validation failure returns 400 Bad Request")
    void testAdminRegister_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/api/auth/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(authService, never()).registerAdmin(any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/admin/login - Success returns 200 OK with AuthResponse")
    void testAdminLogin_Success() throws Exception {
        AuthResponse mockResponse = new AuthResponse("mock-jwt-admin-token", 1, "Priya Sharma", "priya@sewngo.org", "ROLE_SUPER_ADMIN");
        when(authService.loginAdmin(any(AdminLoginRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                  "email": "priya@sewngo.org",
                  "password": "AdminPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-admin-token"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Priya Sharma"))
                .andExpect(jsonPath("$.email").value("priya@sewngo.org"))
                .andExpect(jsonPath("$.role").value("ROLE_SUPER_ADMIN"));

        verify(authService).loginAdmin(any(AdminLoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/admin/login - Blank credentials return 400 Bad Request")
    void testAdminLogin_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(authService, never()).loginAdmin(any());
    }

    @Test
    @DisplayName("POST /api/auth/donor/register - Success returns 201 Created with DonorResponse")
    void testDonorRegister_Success() throws Exception {
        DonorResponse mockResponse = new DonorResponse(5, "Rahul Verma", "rahul@email.com", "9876543210", "Bengaluru", true, LocalDateTime.now());
        when(authService.registerDonor(any(DonorRegisterRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                  "name": "Rahul Verma",
                  "email": "rahul@email.com",
                  "password": "DonorPassword123",
                  "phone": "9876543210",
                  "address": "12 MG Road, Bengaluru"
                }
                """;

        mockMvc.perform(post("/api/auth/donor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorId").value(5))
                .andExpect(jsonPath("$.name").value("Rahul Verma"))
                .andExpect(jsonPath("$.email").value("rahul@email.com"));

        verify(authService).registerDonor(any(DonorRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/donor/register - Validation failure returns 400 Bad Request")
    void testDonorRegister_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "email": "bad-email",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/donor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(authService, never()).registerDonor(any());
    }

    @Test
    @DisplayName("POST /api/auth/donor/login - Success returns 200 OK with AuthResponse")
    void testDonorLogin_Success() throws Exception {
        AuthResponse mockResponse = new AuthResponse("mock-jwt-donor-token", 5, "Rahul Verma", "rahul@email.com", "ROLE_DONOR");
        when(authService.loginDonor(any(DonorLoginRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                  "email": "rahul@email.com",
                  "password": "DonorPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/donor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-donor-token"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Rahul Verma"))
                .andExpect(jsonPath("$.role").value("ROLE_DONOR"));

        verify(authService).loginDonor(any(DonorLoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/donor/login - Validation failure returns 400 Bad Request")
    void testDonorLogin_ValidationFailure() throws Exception {
        String invalidJson = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/donor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(authService, never()).loginDonor(any());
    }
}
