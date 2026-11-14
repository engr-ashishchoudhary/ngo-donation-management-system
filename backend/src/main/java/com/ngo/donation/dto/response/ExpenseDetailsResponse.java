package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseDetailsResponse {

    private Integer expenseId;
    private Integer campaignId;
    private String campaignName;
    private CampaignStatus campaignStatus;
    private Integer adminId;
    private String adminName;
    private AdminRole adminRole;
    private String description;
    private BigDecimal amount;
    private ExpenseCategory category;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;

    public ExpenseDetailsResponse() {
    }

    public ExpenseDetailsResponse(Integer expenseId, Integer campaignId, String campaignName,
                                  Integer adminId, String adminName, String description,
                                  BigDecimal amount, ExpenseCategory category,
                                  LocalDate expenseDate, LocalDateTime createdAt) {
        this(expenseId, campaignId, campaignName, null, adminId, adminName, null,
                description, amount, category, expenseDate, createdAt);
    }

    public ExpenseDetailsResponse(Integer expenseId, Integer campaignId, String campaignName,
                                  CampaignStatus campaignStatus, Integer adminId, String adminName,
                                  AdminRole adminRole, String description, BigDecimal amount,
                                  ExpenseCategory category, LocalDate expenseDate,
                                  LocalDateTime createdAt) {
        this.expenseId = expenseId;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.campaignStatus = campaignStatus;
        this.adminId = adminId;
        this.adminName = adminName;
        this.adminRole = adminRole;
        this.description = description;
        this.amount = amount;
        this.category = category;
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

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public CampaignStatus getCampaignStatus() {
        return campaignStatus;
    }

    public void setCampaignStatus(CampaignStatus campaignStatus) {
        this.campaignStatus = campaignStatus;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public AdminRole getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(AdminRole adminRole) {
        this.adminRole = adminRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
