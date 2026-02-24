package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CampaignTemplateDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.CampaignTemplate;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.CampaignMapper;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.CampaignTemplateRepository;
import com.lealtixservice.service.CampaignTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignTemplateServiceImpl implements CampaignTemplateService {

    private final CampaignTemplateRepository templateRepository;
    private final CampaignRepository campaignRepository;

    @Override
    public List<CampaignTemplateDTO> findAll() {
        return templateRepository.findAll().stream()
                .map(CampaignMapper::toTemplateDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignTemplateDTO> findAllWithUsageStatus(Long tenantId) {
        // Obtener todos los templates
        List<CampaignTemplate> allTemplates = templateRepository.findAll();
        
        // Obtener campañas activas del tenant (status ACTIVE y dentro del rango de fechas)
        List<Campaign> activeCampaigns = campaignRepository.findByBusinessIdAndStatus(tenantId, CampaignStatus.ACTIVE)
                .stream()
                .filter(campaign -> {
                    LocalDate today = LocalDate.now();
                    LocalDate start = campaign.getStartDate();
                    LocalDate end = campaign.getEndDate();
                    boolean afterStart = start == null || !today.isBefore(start);
                    boolean beforeEnd = end == null || !today.isAfter(end);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
        
        // Obtener IDs de templates que están en uso
        Set<Long> templateIdsInUse = activeCampaigns.stream()
                .filter(campaign -> campaign.getTemplate() != null)
                .map(campaign -> campaign.getTemplate().getId())
                .collect(Collectors.toSet());
        
        // Mapear templates a DTO con flag inUse
        return allTemplates.stream()
                .map(template -> {
                    CampaignTemplateDTO dto = CampaignMapper.toTemplateDTO(template);
                    dto.setInUse(templateIdsInUse.contains(template.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CampaignTemplateDTO findById(Long id) {
        CampaignTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + id));
        return CampaignMapper.toTemplateDTO(template);
    }

    @Override
    @Transactional
    public CampaignTemplateDTO create(CampaignTemplateDTO dto) {
        CampaignTemplate entity = CampaignMapper.toTemplateEntity(dto);
        if (entity.getIsActive() == null) entity.setIsActive(true);
        CampaignTemplate saved = templateRepository.save(entity);
        return CampaignMapper.toTemplateDTO(saved);
    }

    @Override
    @Transactional
    public CampaignTemplateDTO update(Long id, CampaignTemplateDTO dto) {
        CampaignTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + id));
        CampaignMapper.updateTemplateEntity(dto, template);
        CampaignTemplate saved = templateRepository.save(template);
        return CampaignMapper.toTemplateDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CampaignTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + id));
        templateRepository.delete(template);
    }
}
