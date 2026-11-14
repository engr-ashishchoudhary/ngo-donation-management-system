package com.ngo.donation.service.impl;

import com.ngo.donation.dto.response.AdminResponse;
import com.ngo.donation.dto.response.DashboardResponse;
import com.ngo.donation.dto.response.DonorResponse;
import com.ngo.donation.mapper.DashboardRowMapper;
import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.DonorRepository;
import com.ngo.donation.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final DonorRepository donorRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdminServiceImpl(AdminRepository adminRepository,
                            DonorRepository donorRepository,
                            JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.donorRepository = donorRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(a -> new AdminResponse(
                        a.getAdminId(),
                        a.getName(),
                        a.getEmail(),
                        a.getRole(),
                        a.getIsActive(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorResponse> getAllDonors() {
        return donorRepository.findAll().stream()
                .map(d -> new DonorResponse(
                        d.getDonorId(),
                        d.getName(),
                        d.getEmail(),
                        d.getPhone(),
                        d.getAddress(),
                        d.getIsActive(),
                        d.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardSummary() {
        String sql = "SELECT * FROM vw_admin_dashboard LIMIT 1";
        List<DashboardResponse> results = jdbcTemplate.query(sql, new DashboardRowMapper());
        if (results.isEmpty()) {
            return new DashboardResponse();
        }
        return results.get(0);
    }
}
