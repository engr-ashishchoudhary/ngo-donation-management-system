package com.ngo.donation.mapper;

import com.ngo.donation.dto.response.DonationDetailsResponse;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.DonationStatus;
import com.ngo.donation.entity.enums.PaymentMethod;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DonationDetailsRowMapper implements RowMapper<DonationDetailsResponse> {

    @Override
    public DonationDetailsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        String paymentMethodStr = rs.getString("payment_method");
        String statusStr = rs.getString("status");
        String campaignStatusStr = rs.getString("campaign_status");
        Timestamp donatedAt = rs.getTimestamp("donated_at");

        return new DonationDetailsResponse(
                rs.getObject("donation_id", Integer.class),
                rs.getObject("donor_id", Integer.class),
                rs.getString("donor_name"),
                rs.getString("donor_email"),
                rs.getObject("campaign_id", Integer.class),
                rs.getString("campaign_name"),
                campaignStatusStr != null ? CampaignStatus.valueOf(campaignStatusStr) : null,
                rs.getBigDecimal("amount"),
                paymentMethodStr != null ? PaymentMethod.valueOf(paymentMethodStr) : null,
                statusStr != null ? DonationStatus.valueOf(statusStr) : null,
                rs.getString("transaction_ref"),
                rs.getString("notes"),
                donatedAt != null ? donatedAt.toLocalDateTime() : null
        );
    }
}
