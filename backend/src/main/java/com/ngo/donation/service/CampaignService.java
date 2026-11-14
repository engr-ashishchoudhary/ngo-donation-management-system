package com.ngo.donation.service;

import com.ngo.donation.dto.request.CampaignRequest;
import com.ngo.donation.dto.response.CampaignFinancialsResponse;
import com.ngo.donation.dto.response.CampaignResponse;
import com.ngo.donation.entity.enums.CampaignStatus;

import java.util.List;

public interface CampaignService {

    List<CampaignFinancialsResponse> getAllCampaignFinancials(CampaignStatus status);

    CampaignFinancialsResponse getCampaignFinancialsById(Integer campaignId);

    CampaignResponse createCampaign(CampaignRequest request, Integer adminId);

    CampaignResponse updateCampaign(Integer campaignId, CampaignRequest request);

    CampaignResponse closeCampaign(Integer campaignId);
}
