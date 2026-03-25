package com.lealtixservice.service;

import com.lealtixservice.dto.CampaignTemplateDTO;
import java.util.List;

public interface CampaignTemplateService {
    List<CampaignTemplateDTO> findAll();
    List<CampaignTemplateDTO> findAllWithUsageStatus(Long tenantId);
    CampaignTemplateDTO findById(Long id);
    CampaignTemplateDTO create(CampaignTemplateDTO dto);
    CampaignTemplateDTO update(Long id, CampaignTemplateDTO dto);
    void delete(Long id);
}