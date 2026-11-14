package com.ngo.donation.mapper;

import com.ngo.donation.dto.response.CampaignFinancialsResponse;
import com.ngo.donation.entity.enums.CampaignStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CampaignFinancialsRowMapper implements RowMapper<CampaignFinancialsResponse> {

    @Override
    public CampaignFinancialsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");
        Timestamp createdAt = rs.getTimestamp("created_at");
        String statusStr = rs.getString("status");

        return new CampaignFinancialsResponse(
                rs.getObject("campaign_id", Integer.class),
                rs.getString("campaign_name"),
                null,
                rs.getBigDecimal("goal_amount"),
                rs.getBigDecimal("total_collected"),
                rs.getBigDecimal("total_spent"),
                rs.getBigDecimal("available_funds"),
                statusStr != null ? CampaignStatus.valueOf(statusStr) : null,
                startDate != null ? startDate.toLocalDate() : null,
                endDate != null ? endDate.toLocalDate() : null,
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
