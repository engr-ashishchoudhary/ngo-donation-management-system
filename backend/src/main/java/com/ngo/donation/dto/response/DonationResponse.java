package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DonationResponse {

    private Integer donationId;
    private Integer campaignId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private DonationStatus status;
    private String transactionRef;
    private LocalDateTime donatedAt;

    public DonationResponse() {
    }

    public DonationResponse(Integer donationId, Integer campaignId, BigDecimal amount,
                            PaymentMethod paymentMethod, DonationStatus status,
                            String transactionRef, LocalDateTime donatedAt) {
        this.donationId = donationId;
        this.campaignId = campaignId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionRef = transactionRef;
        this.donatedAt = donatedAt;
    }

    public Integer getDonationId() {
        return donationId;
    }

    public void setDonationId(Integer donationId) {
        this.donationId = donationId;
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

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public LocalDateTime getDonatedAt() {
        return donatedAt;
    }

    public void setDonatedAt(LocalDateTime donatedAt) {
        this.donatedAt = donatedAt;
    }
}
