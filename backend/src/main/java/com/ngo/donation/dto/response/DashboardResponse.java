package com.ngo.donation.dto.response;

import java.math.BigDecimal;

public class DashboardResponse {

    private Long totalActiveAdmins;
    private Long totalActiveDonors;
    private Long totalCampaigns;
    private Long activeCampaignsCount;

    private BigDecimal grandTotalCollected;
    private BigDecimal grandTotalSpent;
    private BigDecimal grandTotalAvailable;

    private Long totalCompletedDonations;
    private Long totalPendingDonations;
    private Long totalFailedDonations;
    private Long totalRefundedDonations;

    public DashboardResponse() {
    }

    public DashboardResponse(Long totalActiveAdmins, Long totalActiveDonors, Long totalCampaigns,
                             Long activeCampaignsCount, BigDecimal grandTotalCollected,
                             BigDecimal grandTotalSpent, BigDecimal grandTotalAvailable,
                             Long totalCompletedDonations, Long totalPendingDonations,
                             Long totalFailedDonations, Long totalRefundedDonations) {
        this.totalActiveAdmins = totalActiveAdmins;
        this.totalActiveDonors = totalActiveDonors;
        this.totalCampaigns = totalCampaigns;
        this.activeCampaignsCount = activeCampaignsCount;
        this.grandTotalCollected = grandTotalCollected;
        this.grandTotalSpent = grandTotalSpent;
        this.grandTotalAvailable = grandTotalAvailable;
        this.totalCompletedDonations = totalCompletedDonations;
        this.totalPendingDonations = totalPendingDonations;
        this.totalFailedDonations = totalFailedDonations;
        this.totalRefundedDonations = totalRefundedDonations;
    }

    public Long getTotalActiveAdmins() {
        return totalActiveAdmins;
    }

    public void setTotalActiveAdmins(Long totalActiveAdmins) {
        this.totalActiveAdmins = totalActiveAdmins;
    }

    public Long getTotalActiveDonors() {
        return totalActiveDonors;
    }

    public void setTotalActiveDonors(Long totalActiveDonors) {
        this.totalActiveDonors = totalActiveDonors;
    }

    public Long getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(Long totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public Long getActiveCampaignsCount() {
        return activeCampaignsCount;
    }

    public void setActiveCampaignsCount(Long activeCampaignsCount) {
        this.activeCampaignsCount = activeCampaignsCount;
    }

    public BigDecimal getGrandTotalCollected() {
        return grandTotalCollected;
    }

    public void setGrandTotalCollected(BigDecimal grandTotalCollected) {
        this.grandTotalCollected = grandTotalCollected;
    }

    public BigDecimal getGrandTotalSpent() {
        return grandTotalSpent;
    }

    public void setGrandTotalSpent(BigDecimal grandTotalSpent) {
        this.grandTotalSpent = grandTotalSpent;
    }

    public BigDecimal getGrandTotalAvailable() {
        return grandTotalAvailable;
    }

    public void setGrandTotalAvailable(BigDecimal grandTotalAvailable) {
        this.grandTotalAvailable = grandTotalAvailable;
    }

    public Long getTotalCompletedDonations() {
        return totalCompletedDonations;
    }

    public void setTotalCompletedDonations(Long totalCompletedDonations) {
        this.totalCompletedDonations = totalCompletedDonations;
    }

    public Long getTotalPendingDonations() {
        return totalPendingDonations;
    }

    public void setTotalPendingDonations(Long totalPendingDonations) {
        this.totalPendingDonations = totalPendingDonations;
    }

    public Long getTotalFailedDonations() {
        return totalFailedDonations;
    }

    public void setTotalFailedDonations(Long totalFailedDonations) {
        this.totalFailedDonations = totalFailedDonations;
    }

    public Long getTotalRefundedDonations() {
        return totalRefundedDonations;
    }

    public void setTotalRefundedDonations(Long totalRefundedDonations) {
        this.totalRefundedDonations = totalRefundedDonations;
    }
}
