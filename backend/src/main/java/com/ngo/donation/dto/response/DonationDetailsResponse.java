package com.ngo.donation.dto.response;

import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DonationDetailsResponse {

    private Integer donationId;
    private Integer donorId;
    private String donorName;
    private String donorEmail;
    private Integer campaignId;
    private String campaignName;
    private CampaignStatus campaignStatus;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private DonationStatus status;
    private String transactionRef;
    private String notes;
    private LocalDateTime donatedAt;

    public DonationDetailsResponse() {
    }

    public DonationDetailsResponse(Integer donationId, Integer donorId, String donorName,
                                   String donorEmail, Integer campaignId, String campaignName,
                                   BigDecimal amount, PaymentMethod paymentMethod,
                                   DonationStatus status, String transactionRef,
                                   String notes, LocalDateTime donatedAt) {
        this(donationId, donorId, donorName, donorEmail, campaignId, campaignName,
                null, amount, paymentMethod, status, transactionRef, notes, donatedAt);
    }

    public DonationDetailsResponse(Integer donationId, Integer donorId, String donorName,
                                   String donorEmail, Integer campaignId, String campaignName,
                                   CampaignStatus campaignStatus, BigDecimal amount,
                                   PaymentMethod paymentMethod, DonationStatus status,
                                   String transactionRef, String notes, LocalDateTime donatedAt) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.campaignStatus = campaignStatus;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionRef = transactionRef;
        this.notes = notes;
        this.donatedAt = donatedAt;
    }

    public Integer getDonationId() {
        return donationId;
    }

    public void setDonationId(Integer donationId) {
        this.donationId = donationId;
    }

    public Integer getDonorId() {
        return donorId;
    }

    public void setDonorId(Integer donorId) {
        this.donorId = donorId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getDonatedAt() {
        return donatedAt;
    }

    public void setDonatedAt(LocalDateTime donatedAt) {
        this.donatedAt = donatedAt;
    }
}
