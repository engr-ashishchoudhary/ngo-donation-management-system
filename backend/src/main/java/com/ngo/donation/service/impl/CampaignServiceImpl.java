package com.ngo.donation.service.impl;

import com.ngo.donation.dto.request.CampaignRequest;
import com.ngo.donation.dto.response.CampaignFinancialsResponse;
import com.ngo.donation.dto.response.CampaignResponse;
import com.ngo.donation.entity.Admin;
import com.ngo.donation.entity.Campaign;
import com.ngo.donation.entity.enums.CampaignStatus;
import com.ngo.donation.exception.BadRequestException;
import com.ngo.donation.exception.ConflictException;
import com.ngo.donation.exception.InvalidOperationException;
import com.ngo.donation.exception.ResourceNotFoundException;
import com.ngo.donation.mapper.CampaignFinancialsRowMapper;
import com.ngo.donation.repository.AdminRepository;
import com.ngo.donation.repository.CampaignRepository;
import com.ngo.donation.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdminRepository adminRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               AdminRepository adminRepository,
                               JdbcTemplate jdbcTemplate) {
        this.campaignRepository = campaignRepository;
        this.adminRepository = adminRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignFinancialsResponse> getAllCampaignFinancials(CampaignStatus status) {
        String sql = "SELECT * FROM vw_campaign_financials WHERE (? IS NULL OR status = ?) ORDER BY start_date DESC";
        String statusParam = status != null ? status.name() : null;
        return jdbcTemplate.query(sql, new CampaignFinancialsRowMapper(), statusParam, statusParam);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignFinancialsResponse getCampaignFinancialsById(Integer campaignId) {
        String sql = "SELECT * FROM vw_campaign_financials WHERE campaign_id = ?";
        List<CampaignFinancialsResponse> results = jdbcTemplate.query(sql, new CampaignFinancialsRowMapper(), campaignId);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + campaignId);
        }
        return results.get(0);
    }

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request, Integer adminId) {
        if (campaignRepository.existsByName(request.getName())) {
            throw new ConflictException("Campaign name is already in use: " + request.getName());
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be strictly after start date.");
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        CampaignStatus initialStatus = request.getStatus() != null ? request.getStatus() : CampaignStatus.ACTIVE;

        Campaign campaign = new Campaign(
                admin,
                request.getName(),
                request.getDescription(),
                request.getGoalAmount(),
                initialStatus,
                request.getStartDate(),
                request.getEndDate()
        );

        Campaign saved = campaignRepository.save(campaign);

        return new CampaignResponse(
                saved.getCampaignId(),
                saved.getName(),
                saved.getDescription(),
                saved.getGoalAmount(),
                saved.getStatus(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Integer campaignId, CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (campaign.getStatus() == CampaignStatus.CLOSED || campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new InvalidOperationException("Cannot update campaign in terminal status: " + campaign.getStatus());
        }

        if (!campaign.getName().equalsIgnoreCase(request.getName()) && campaignRepository.existsByName(request.getName())) {
            throw new ConflictException("Campaign name is already in use: " + request.getName());
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be strictly after start date.");
        }

        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setGoalAmount(request.getGoalAmount());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());

        if (request.getStatus() != null) {
            if (request.getStatus() != CampaignStatus.ACTIVE && request.getStatus() != CampaignStatus.CLOSED && request.getStatus() != CampaignStatus.COMPLETED) {
                throw new InvalidOperationException("Invalid status transition for campaign: " + request.getStatus());
            }
            campaign.setStatus(request.getStatus());
        }

        Campaign updated = campaignRepository.save(campaign);

        return new CampaignResponse(
                updated.getCampaignId(),
                updated.getName(),
                updated.getDescription(),
                updated.getGoalAmount(),
                updated.getStatus(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public CampaignResponse closeCampaign(Integer campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new InvalidOperationException("Only ACTIVE campaigns can be closed. Current status: " + campaign.getStatus());
        }

        campaign.setStatus(CampaignStatus.CLOSED);
        Campaign updated = campaignRepository.save(campaign);

        return new CampaignResponse(
                updated.getCampaignId(),
                updated.getName(),
                updated.getDescription(),
                updated.getGoalAmount(),
                updated.getStatus(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getCreatedAt()
        );
    }
}
