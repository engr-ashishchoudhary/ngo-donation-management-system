package com.ngo.donation.dto.request;

import com.ngo.donation.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class DonationRequest {

    @NotNull(message = "Campaign ID is required")
    @Positive(message = "Campaign ID must be a positive integer")
    private Integer campaignId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Donation amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionRef;

    private String notes;

    public DonationRequest() {
    }

    public DonationRequest(Integer campaignId, BigDecimal amount, PaymentMethod paymentMethod,
                           String transactionRef, String notes) {
        this.campaignId = campaignId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
        this.notes = notes;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
