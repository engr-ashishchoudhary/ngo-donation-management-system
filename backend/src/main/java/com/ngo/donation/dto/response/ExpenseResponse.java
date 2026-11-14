package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseResponse {

    private Integer expenseId;
    private Integer campaignId;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String description;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;

    public ExpenseResponse() {
    }

    public ExpenseResponse(Integer expenseId, Integer campaignId, BigDecimal amount,
                           ExpenseCategory category, String description,
                           LocalDate expenseDate, LocalDateTime createdAt) {
        this.expenseId = expenseId;
        this.campaignId = campaignId;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    public Integer getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Integer expenseId) {
        this.expenseId = expenseId;
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Integer campaignId) {
        this.campaignId = campaignId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
