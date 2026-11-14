package com.ngo.donation.mapper;

import com.ngo.donation.dto.response.ExpenseDetailsResponse;
import com.ngo.donation.entity.enums.AdminRole;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.entity.enums.ExpenseCategory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ExpenseDetailsRowMapper implements RowMapper<ExpenseDetailsResponse> {

    @Override
    public ExpenseDetailsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        String campaignStatusStr = rs.getString("campaign_status");
        String adminRoleStr = rs.getString("admin_role");
        String categoryStr = rs.getString("category");
        Date expenseDate = rs.getDate("expense_date");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new ExpenseDetailsResponse(
                rs.getObject("expense_id", Integer.class),
                rs.getObject("campaign_id", Integer.class),
                rs.getString("campaign_name"),
                campaignStatusStr != null ? CampaignStatus.valueOf(campaignStatusStr) : null,
                rs.getObject("admin_id", Integer.class),
                rs.getString("admin_name"),
                adminRoleStr != null ? AdminRole.valueOf(adminRoleStr) : null,
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                categoryStr != null ? ExpenseCategory.valueOf(categoryStr) : null,
                expenseDate != null ? expenseDate.toLocalDate() : null,
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
