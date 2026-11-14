package com.ngo.donation.dto.request;

import com.ngo.donation.entity.enums.CampaignStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(max = 200, message = "Campaign name must not exceed 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Goal amount is required")
    @DecimalMin(value = "0.01", message = "Goal amount must be greater than 0")
    private BigDecimal goalAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private CampaignStatus status;

    public CampaignRequest() {
    }

    public CampaignRequest(String name, String description, BigDecimal goalAmount,
                           LocalDate startDate, LocalDate endDate, CampaignStatus status) {
        this.name = name;
        this.description = description;
        this.goalAmount = goalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    @AssertTrue(message = "End date must be strictly after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
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

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }
}
