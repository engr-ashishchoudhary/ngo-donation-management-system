package com.ngo.donation.mapper;

import com.ngo.donation.dto.response.DashboardResponse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardRowMapper implements RowMapper<DashboardResponse> {

    @Override
    public DashboardResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DashboardResponse(
                rs.getObject("total_active_admins", Long.class),
                rs.getObject("total_active_donors", Long.class),
                rs.getObject("total_campaigns", Long.class),
                rs.getObject("active_campaigns_count", Long.class),
                rs.getBigDecimal("grand_total_collected"),
                rs.getBigDecimal("grand_total_spent"),
                rs.getBigDecimal("grand_total_available"),
                rs.getObject("total_completed_donations", Long.class),
                rs.getObject("total_pending_donations", Long.class),
                rs.getObject("total_failed_donations", Long.class),
                rs.getObject("total_refunded_donations", Long.class)
        );
    }
}
