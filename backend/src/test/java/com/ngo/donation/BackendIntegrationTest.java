package com.ngo.donation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ngo.donation.dto.request.*;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.ExpenseCategory;
import com.ngo.donation.entity.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // Shared state across sequential integration test flows
    private static String superAdminToken;
    private static String donor1Token;
    private static Integer donor1Id;
    private static String donor1Email;

    private static String donor2Token;
    private static Integer donor2Id;
    private static String donor2Email;

    private static Integer createdCampaignId;
    private static Integer createdDonationId;

    // =========================================================================
    // Flow 1: Public Campaign Listing (No Auth)
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Flow 1: Public campaign listing succeeds without authentication")
    void test01_publicCampaignListing() throws Exception {
        mockMvc.perform(get("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    // =========================================================================
    // Flow 2: Donor Registration and Login (Generates Real JWT)
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Flow 2: Donor registration and login produces authoritative JWT token")
    void test02_donorRegistrationAndLogin() throws Exception {
        donor1Email = "integration_donor_" + UUID.randomUUID().toString().substring(0, 8) + "@test.org";

        // Register Donor 1
        DonorRegisterRequest registerRequest = new DonorRegisterRequest();
        registerRequest.setName("Integration Donor One");
        registerRequest.setEmail(donor1Email);
        registerRequest.setPassword("Donor@Test1234");
        registerRequest.setPhone("9876543210");
        registerRequest.setAddress("123 Test Street, Bangalore");

        MvcResult regResult = mockMvc.perform(post("/api/auth/donor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorId", notNullValue()))
                .andExpect(jsonPath("$.email", is(donor1Email)))
                .andExpect(jsonPath("$.name", is("Integration Donor One")))
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        donor1Id = regNode.get("donorId").asInt();

        // Login Donor 1
        DonorLoginRequest loginRequest = new DonorLoginRequest();
        loginRequest.setEmail(donor1Email);
        loginRequest.setPassword("Donor@Test1234");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/donor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role", is("DONOR")))
                .andExpect(jsonPath("$.id", is(donor1Id)))
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        donor1Token = loginNode.get("token").stringValue();
        assertNotNull(donor1Token);
    }

    // =========================================================================
    // Flow 3: Real JWT Authentication Using Login Token
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("Flow 3: Real JWT authentication succeeds for authenticated donor endpoint")
    void test03_jwtAuthenticationWithBearerToken() throws Exception {
        assertNotNull(donor1Token, "Donor 1 token must be present from Flow 2");

        mockMvc.perform(get("/api/donations/my")
                        .header("Authorization", "Bearer " + donor1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", is(0))); // No donations created yet
    }

    // =========================================================================
    // Flow 4: Missing Token Returns 401 Unauthorized
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Flow 4: Missing token on protected endpoint returns 401 Unauthorized")
    void test04_missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/donations/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.path", is("/api/donations/my")));
    }

    // =========================================================================
    // Flow 5: Donor Accessing Admin-Only Endpoint Returns 403 Forbidden
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("Flow 5: Donor token accessing admin endpoint returns 403 Forbidden")
    void test05_donorAccessingAdminEndpointReturns403() throws Exception {
        assertNotNull(donor1Token);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + donor1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.path", is("/api/admin/dashboard")));
    }

    // =========================================================================
    // Flow 6: Protected Campaign Creation (Admin Auth)
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("Flow 6: Authenticated SUPER_ADMIN creates a new campaign")
    void test06_createCampaignByAdmin() throws Exception {
        // Authenticate initial seeded Super Admin (bootstrapped with SuperAdmin@Test123)
        AdminLoginRequest adminLogin = new AdminLoginRequest();
        adminLogin.setEmail("priya.sharma@sewngo.org");
        adminLogin.setPassword("SuperAdmin@Test123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role", is("SUPER_ADMIN")))
                .andReturn();

        JsonNode adminNode = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        superAdminToken = adminNode.get("token").stringValue();
        assertNotNull(superAdminToken);

        // Create a new test campaign
        CampaignRequest campaignRequest = new CampaignRequest();
        campaignRequest.setName("Integration Test Flood Relief " + UUID.randomUUID().toString().substring(0, 6));
        campaignRequest.setDescription("Campaign created during backend integration test execution");
        campaignRequest.setGoalAmount(new BigDecimal("100000.00"));
        campaignRequest.setStartDate(LocalDate.now());
        campaignRequest.setEndDate(LocalDate.now().plusMonths(3));
        campaignRequest.setStatus(CampaignStatus.ACTIVE);

        MvcResult createResult = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId", notNullValue()))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.goalAmount", is(100000.00)))
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        createdCampaignId = createNode.get("campaignId").asInt();
        assertTrue(createdCampaignId > 0);
    }

    // =========================================================================
    // Flow 7: Donation Creation by Authenticated Donor
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("Flow 7: Authenticated donor records a donation against the active campaign")
    void test07_recordDonationByDonor() throws Exception {
        assertNotNull(donor1Token);
        assertNotNull(createdCampaignId);

        DonationRequest donationRequest = new DonationRequest();
        donationRequest.setCampaignId(createdCampaignId);
        donationRequest.setAmount(new BigDecimal("5000.00"));
        donationRequest.setPaymentMethod(PaymentMethod.UPI);
        donationRequest.setTransactionRef("UPI-TEST-" + System.currentTimeMillis());
        donationRequest.setNotes("Integration test donation note");

        MvcResult result = mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + donor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(donationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donationId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(5000.00)))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        createdDonationId = node.get("donationId").asInt();
        assertTrue(createdDonationId > 0);
    }

    // =========================================================================
    // Flow 8: Cross-Donor Donation Access is Forbidden
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Flow 8: Cross-donor access to private donation details returns 403 Forbidden")
    void test08_crossDonorAccessForbidden() throws Exception {
        assertNotNull(createdDonationId);

        // Register Donor 2
        donor2Email = "integration_donor2_" + UUID.randomUUID().toString().substring(0, 8) + "@test.org";
        DonorRegisterRequest regRequest = new DonorRegisterRequest();
        regRequest.setName("Integration Donor Two");
        regRequest.setEmail(donor2Email);
        regRequest.setPassword("Donor2@Test1234");

        MvcResult regResult = mockMvc.perform(post("/api/auth/donor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        donor2Id = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("donorId").asInt();

        // Login Donor 2
        DonorLoginRequest loginRequest = new DonorLoginRequest();
        loginRequest.setEmail(donor2Email);
        loginRequest.setPassword("Donor2@Test1234");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/donor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        donor2Token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").stringValue();
        assertNotNull(donor2Token);

        // Donor 2 attempts to view Donor 1's donation by ID -> MUST be 403 Forbidden
        mockMvc.perform(get("/api/donations/" + createdDonationId)
                        .header("Authorization", "Bearer " + donor2Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    // =========================================================================
    // Flow 9: Admin Donation Status Update & Database Triggers
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Flow 9: Admin updates donation status to COMPLETED, firing DB triggers")
    void test09_updateDonationStatusExecutesTrigger() throws Exception {
        assertNotNull(superAdminToken);
        assertNotNull(createdDonationId);

        // Transition from PENDING -> COMPLETED
        DonationStatusUpdateRequest updateRequest = new DonationStatusUpdateRequest();
        updateRequest.setStatus(DonationStatus.COMPLETED);

        mockMvc.perform(patch("/api/donations/" + createdDonationId + "/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donationId", is(createdDonationId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Verify campaign financials reflect collected donation
        mockMvc.perform(get("/api/campaigns/" + createdCampaignId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollected", is(5000.00)))
                .andExpect(jsonPath("$.availableFunds", is(5000.00)));
    }

    // =========================================================================
    // Flow 10: Expense Creation Executes sp_record_expense
    // =========================================================================
    @Test
    @Order(10)
    @DisplayName("Flow 10: Expense recording executes MySQL stored procedure sp_record_expense")
    void test10_recordExpenseExecutesProcedure() throws Exception {
        assertNotNull(superAdminToken);
        assertNotNull(createdCampaignId);

        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Relief food packets distribution");
        expenseRequest.setAmount(new BigDecimal("2000.00"));
        expenseRequest.setCategory(ExpenseCategory.SUPPLIES);
        expenseRequest.setExpenseDate(LocalDate.now());

        mockMvc.perform(post("/api/campaigns/" + createdCampaignId + "/expenses")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseId", notNullValue()))
                .andExpect(jsonPath("$.amount", is(2000.00)))
                .andExpect(jsonPath("$.campaignId", is(createdCampaignId)))
                .andExpect(jsonPath("$.category", is("SUPPLIES")));

        // Verify remaining available funds via view
        mockMvc.perform(get("/api/campaigns/" + createdCampaignId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollected", is(5000.00)))
                .andExpect(jsonPath("$.totalSpent", is(2000.00)))
                .andExpect(jsonPath("$.availableFunds", is(3000.00)));
    }

    // =========================================================================
    // Flow 11: Insufficient Campaign Funds Returns 422
    // =========================================================================
    @Test
    @Order(11)
    @DisplayName("Flow 11: Expense exceeding available funds is rejected by procedure with 422")
    void test11_insufficientCampaignFundsReturns422() throws Exception {
        assertNotNull(superAdminToken);
        assertNotNull(createdCampaignId);

        // Available funds are 3000.00; attempt to spend 10000.00
        ExpenseRequest overspendRequest = new ExpenseRequest();
        overspendRequest.setDescription("Excessive expense attempt");
        overspendRequest.setAmount(new BigDecimal("10000.00"));
        overspendRequest.setCategory(ExpenseCategory.OPERATIONS);
        overspendRequest.setExpenseDate(LocalDate.now());

        mockMvc.perform(post("/api/campaigns/" + createdCampaignId + "/expenses")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overspendRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("Unprocessable Entity")))
                .andExpect(jsonPath("$.message", containsString("exceeds available campaign funds")));
    }

    // =========================================================================
    // Flow 12: Admin Dashboard Reads Database View
    // =========================================================================
    @Test
    @Order(12)
    @DisplayName("Flow 12: Admin dashboard queries vw_admin_dashboard database view")
    void test12_adminDashboardReadsView() throws Exception {
        assertNotNull(superAdminToken);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCampaigns", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalCompletedDonations", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.grandTotalCollected", greaterThanOrEqualTo(5000.00)))
                .andExpect(jsonPath("$.grandTotalSpent", greaterThanOrEqualTo(2000.00)));
    }

    // =========================================================================
    // Flow 13: Donor Profile Retrieval and Update
    // =========================================================================
    @Test
    @Order(13)
    @DisplayName("Flow 13: Donor retrieves and updates profile details")
    void test13_donorProfileRetrievalAndUpdate() throws Exception {
        assertNotNull(donor1Token);

        // Get current profile
        mockMvc.perform(get("/api/donor/profile")
                        .header("Authorization", "Bearer " + donor1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(donor1Email)))
                .andExpect(jsonPath("$.name", is("Integration Donor One")));

        // Update profile
        DonorProfileUpdateRequest updateRequest = new DonorProfileUpdateRequest();
        updateRequest.setName("Integration Donor One Updated");
        updateRequest.setPhone("9123456789");
        updateRequest.setAddress("456 New Road, Indiranagar, Bangalore");

        mockMvc.perform(put("/api/donor/profile")
                        .header("Authorization", "Bearer " + donor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Integration Donor One Updated")))
                .andExpect(jsonPath("$.phone", is("9123456789")))
                .andExpect(jsonPath("$.address", is("456 New Road, Indiranagar, Bangalore")));
    }

    // =========================================================================
    // Flow 14: Validation Errors Return Standardized ErrorResponse
    // =========================================================================
    @Test
    @Order(14)
    @DisplayName("Flow 14: Invalid request triggers Bean Validation returning ErrorResponse (400)")
    void test14_validationErrorReturnsStandardizedResponse() throws Exception {
        DonorRegisterRequest invalidRequest = new DonorRegisterRequest();
        invalidRequest.setName(""); // Invalid: blank
        invalidRequest.setEmail("not-an-email"); // Invalid: malformed
        invalidRequest.setPassword("short"); // Invalid: too short

        mockMvc.perform(post("/api/auth/donor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/auth/donor/register")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
