package com.ngo.donation.controller;

import com.ngo.donation.dto.request.DonorProfileUpdateRequest;
import com.ngo.donation.dto.response.DonorProfileResponse;
import com.ngo.donation.exception.GlobalExceptionHandler;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.DonorService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DonorControllerTest {

    @Mock
    private DonorService donorService;

    @InjectMocks
    private DonorController donorController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(donorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/donor/profile - Success returns 200 OK and extracts donor ID from UserPrincipal")
    void testGetDonorProfile_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        DonorProfileResponse mockProfile = new DonorProfileResponse(
                5, "Rahul Verma", "rahul@email.com", "9876543210", "Bengaluru",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(donorService.getDonorProfile(5)).thenReturn(mockProfile);

        mockMvc.perform(get("/api/donor/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorId").value(5))
                .andExpect(jsonPath("$.name").value("Rahul Verma"))
                .andExpect(jsonPath("$.email").value("rahul@email.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"));

        verify(donorService).getDonorProfile(5);
    }

    @Test
    @DisplayName("GET /api/donor/profile - Donor not found returns 404 Not Found")
    void testGetDonorProfile_NotFound() throws Exception {
        UserPrincipal principal = new UserPrincipal(99, "unknown@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(donorService.getDonorProfile(99))
                .thenThrow(new ResourceNotFoundException("Donor not found with ID: 99"));

        mockMvc.perform(get("/api/donor/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Donor not found with ID: 99"));

        verify(donorService).getDonorProfile(99);
    }

    @Test
    @DisplayName("PUT /api/donor/profile - Success returns 200 OK and extracts donor ID from UserPrincipal")
    void testUpdateDonorProfile_Success() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        DonorProfileResponse updatedProfile = new DonorProfileResponse(
                5, "Rahul Verma Updated", "rahul@email.com", "9123456789", "Mumbai",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(donorService.updateDonorProfile(eq(5), any(DonorProfileUpdateRequest.class)))
                .thenReturn(updatedProfile);

        String json = """
                {
                  "name": "Rahul Verma Updated",
                  "phone": "9123456789",
                  "address": "Mumbai"
                }
                """;

        mockMvc.perform(put("/api/donor/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorId").value(5))
                .andExpect(jsonPath("$.name").value("Rahul Verma Updated"))
                .andExpect(jsonPath("$.phone").value("9123456789"))
                .andExpect(jsonPath("$.address").value("Mumbai"));

        verify(donorService).updateDonorProfile(eq(5), any(DonorProfileUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/donor/profile - Validation failure returns 400 Bad Request")
    void testUpdateDonorProfile_ValidationFailure() throws Exception {
        UserPrincipal principal = new UserPrincipal(5, "rahul@email.com", "ROLE_DONOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        String invalidJson = """
                {
                  "name": ""
                }
                """;

        mockMvc.perform(put("/api/donor/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(donorService, never()).updateDonorProfile(any(), any());
    }
}
