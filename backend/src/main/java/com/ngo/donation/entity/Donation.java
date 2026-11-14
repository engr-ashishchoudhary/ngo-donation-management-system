package com.ngo.donation.entity;

import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Integer donationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DonationStatus status = DonationStatus.PENDING;

    @Column(name = "transaction_ref", unique = true, length = 100)
    private String transactionRef;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "donated_at", nullable = false, updatable = false)
    private LocalDateTime donatedAt;

    public Donation() {
    }

    public Donation(Donor donor, Campaign campaign, BigDecimal amount, PaymentMethod paymentMethod,
                    DonationStatus status, String transactionRef, String notes) {
        this.donor = donor;
        this.campaign = campaign;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status != null ? status : DonationStatus.PENDING;
        this.transactionRef = transactionRef;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.donatedAt == null) {
            this.donatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = DonationStatus.PENDING;
        }
    }

    public Integer getDonationId() {
        return donationId;
    }

    public void setDonationId(Integer donationId) {
        this.donationId = donationId;
    }

    public Donor getDonor() {
        return donor;
    }

    public void setDonor(Donor donor) {
        this.donor = donor;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Donation donation = (Donation) o;
        return donationId != null && donationId.equals(donation.donationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(donationId);
    }

    @Override
    public String toString() {
        return "Donation{" +
                "donationId=" + donationId +
                ", amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", status=" + status +
                ", transactionRef='" + transactionRef + '\'' +
                ", donatedAt=" + donatedAt +
                '}';
    }
}
