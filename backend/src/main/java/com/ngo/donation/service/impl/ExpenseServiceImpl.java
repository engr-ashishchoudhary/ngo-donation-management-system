package com.ngo.donation.service.impl;

import com.ngo.donation.dto.request.ExpenseRequest;
import com.ngo.donation.dto.response.ExpenseDetailsResponse;
import com.ngo.donation.dto.response.ExpenseResponse;
import com.ngo.donation.exception.BadRequestException;
import com.ngo.donation.exception.InsufficientFundsException;
import com.ngo.donation.exception.InvalidOperationException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.mapper.ExpenseDetailsRowMapper;
import com.ngo.donation.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExpenseServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDetailsResponse> getExpensesByCampaign(Integer campaignId) {
        String sql = "SELECT * FROM vw_expense_details WHERE campaign_id = ? ORDER BY expense_date DESC";
        return jdbcTemplate.query(sql, new ExpenseDetailsRowMapper(), campaignId);
    }

    @Override
    // STRICTLY NON-TRANSACTIONAL: sp_record_expense manages its own transaction and pessimistic lock
    public ExpenseResponse recordExpense(Integer campaignId, ExpenseRequest request, Integer adminId) {
        String callSql = "{CALL sp_record_expense(?, ?, ?, ?, ?, ?, ?, ?)}";

        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setInt(1, campaignId);
                cs.setInt(2, adminId);
                cs.setString(3, request.getDescription());
                cs.setBigDecimal(4, request.getAmount());
                cs.setString(5, request.getCategory().name());
                cs.setDate(6, java.sql.Date.valueOf(request.getExpenseDate()));

                cs.registerOutParameter(7, Types.INTEGER);
                cs.registerOutParameter(8, Types.VARCHAR);

                cs.execute();

                int expenseId = cs.getInt(7);
                boolean isExpenseIdNull = cs.wasNull();
                String message = cs.getString(8);

                if (isExpenseIdNull || expenseId <= 0 || (message != null && message.startsWith("ERROR:"))) {
                    if (message != null && message.contains("not found")) {
                        throw new ResourceNotFoundException(message);
                    } else if (message != null && message.contains("exceeds available campaign funds")) {
                        throw new InsufficientFundsException(message);
                    } else if (message != null && (message.contains("cannot be before the campaign start date")
                            || message.contains("must be greater than 0"))) {
                        throw new BadRequestException(message);
                    } else {
                        throw new InvalidOperationException(message != null ? message : "Failed to record expense.");
                    }
                }

                return new ExpenseResponse(
                        expenseId,
                        campaignId,
                        request.getAmount(),
                        request.getCategory(),
                        request.getDescription(),
                        request.getExpenseDate(),
                        LocalDateTime.now()
                );
            }
        });
    }
}
