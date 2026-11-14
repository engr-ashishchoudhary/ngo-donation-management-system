package com.ngo.donation.service;

import com.ngo.donation.dto.request.ExpenseRequest;
import com.ngo.donation.dto.response.ExpenseDetailsResponse;
import com.ngo.donation.dto.response.ExpenseResponse;

import java.util.List;

public interface ExpenseService {

    List<ExpenseDetailsResponse> getExpensesByCampaign(Integer campaignId);

    ExpenseResponse recordExpense(Integer campaignId, ExpenseRequest request, Integer adminId);
}
