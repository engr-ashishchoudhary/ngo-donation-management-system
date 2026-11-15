package com.ngo.donation.controller;

import com.ngo.donation.dto.request.ExpenseRequest;
import com.ngo.donation.dto.response.ExpenseDetailsResponse;
import com.ngo.donation.dto.response.ExpenseResponse;
import com.ngo.donation.security.UserPrincipal;
import com.ngo.donation.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDetailsResponse>> getExpensesByCampaign(
            @PathVariable("campaignId") Integer campaignId) {
        List<ExpenseDetailsResponse> expenses = expenseService.getExpensesByCampaign(campaignId);
        return ResponseEntity.ok(expenses);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> recordExpense(
            @PathVariable("campaignId") Integer campaignId,
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer adminId = principal != null ? principal.getId() : null;
        ExpenseResponse response = expenseService.recordExpense(campaignId, request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
