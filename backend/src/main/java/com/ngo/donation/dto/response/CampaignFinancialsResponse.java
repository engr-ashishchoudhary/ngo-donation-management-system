package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.CampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampaignFinancialsResponse {

    private Integer campaignId;
    private String name;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal totalCollected;
    private BigDecimal totalSpent;
    private BigDecimal availableFunds;
    private CampaignStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public CampaignFinancialsResponse() {
    }

    public CampaignFinancialsResponse(Integer campaignId, String name, String description,
                                      BigDecimal goalAmount, BigDecimal totalCollected,
                                      BigDecimal totalSpent, BigDecimal availableFunds,
                                      CampaignStatus status, LocalDate startDate,
                                      LocalDate endDate, LocalDateTime createdAt) {
        this.campaignId = campaignId;
        this.name = name;
        this.description = description;
        this.goalAmount = goalAmount;
        this.totalCollected = totalCollected;
        this.totalSpent = totalSpent;
        this.availableFunds = availableFunds;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Integer campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(BigDecimal goalAmount) {
        this.goalAmount = goalAmount;
    }

    public BigDecimal getTotalCollected() {
        return totalCollected;
    }

    public void setTotalCollected(BigDecimal totalCollected) {
        this.totalCollected = totalCollected;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public BigDecimal getAvailableFunds() {
        return availableFunds;
    }

    public void setAvailableFunds(BigDecimal availableFunds) {
        this.availableFunds = availableFunds;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
