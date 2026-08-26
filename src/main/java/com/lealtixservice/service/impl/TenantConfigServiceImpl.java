package com.lealtixservice.service.impl;

import com.lealtixservice.dto.TenantConfigDTO;
import com.lealtixservice.entity.TenantConfig;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.repository.TenantConfigRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.TenantConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantConfigServiceImpl implements TenantConfigService {

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantRepository tenantRepository;

    @Override
    public TenantConfigDTO saveTenantConfig(TenantConfigDTO dto) {
        Tenant tenant = tenantRepository.findById(dto.getTenantId()).orElse(null);
        TenantConfig tenantConfig = null;
        if (tenant == null) return null;
        tenantConfig = tenantConfigRepository.findByTenantId(tenant.getId());
        if(tenantConfig != null) {
            tenantConfig.setBussinesEmail(dto.getBussinesEmail() == null || dto.getBussinesEmail().isBlank() ? tenantConfig.getBussinesEmail() : dto.getBussinesEmail());
            tenantConfig.setHistory(dto.getHistory() == null || dto.getHistory().isBlank() ? tenantConfig.getHistory() : dto.getHistory());
            tenantConfig.setVision(dto.getVision() == null || dto.getVision().isBlank() ? tenantConfig.getVision() : dto.getVision());
            tenantConfig.setFacebook(dto.getFacebook() == null || dto.getFacebook().isBlank() ? tenantConfig.getFacebook() : dto.getFacebook());
            tenantConfig.setInstagram(dto.getInstagram() == null || dto.getInstagram().isBlank() ? tenantConfig.getInstagram() : dto.getInstagram());
            tenantConfig.setLinkedin(dto.getLinkedin() == null || dto.getLinkedin().isBlank() ? tenantConfig.getLinkedin() : dto.getLinkedin());
            tenantConfig.setTiktok(dto.getTiktok() == null || dto.getTiktok().isBlank() ? tenantConfig.getTiktok() : dto.getTiktok());
            tenantConfig.setTwitter(dto.getTwitter() == null || dto.getTwitter().isBlank() ? tenantConfig.getTwitter() : dto.getTwitter());
            if (dto.getKitchenModuleEnabled() != null) {
                tenantConfig.setKitchenModuleEnabled(dto.getKitchenModuleEnabled());
                if (dto.getKitchenModuleEnabled() && tenantConfig.getKitchenEnabledAt() == null) {
                    tenantConfig.setKitchenEnabledAt(java.time.LocalDateTime.now());
                }
            }
        }else{
            tenantConfig = toEntity(dto, tenant);
        }
        TenantConfig saved = tenantConfigRepository.save(tenantConfig);
        return toDTO(saved);
    }

    @Override
    public TenantConfigDTO getTenantConfigById(Long id) {
        return tenantConfigRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<TenantConfigDTO> getTenantConfigsByTenantId(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return List.of();
        return tenantConfigRepository.findByTenant(tenant)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private TenantConfigDTO toDTO(TenantConfig entity) {
        return TenantConfigDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant().getId())
                .history(entity.getHistory())
                .vision(entity.getVision())
                .bussinesEmail(entity.getBussinesEmail())
                .twitter(entity.getTwitter())
                .facebook(entity.getFacebook())
                .linkedin(entity.getLinkedin())
                .instagram(entity.getInstagram())
                .tiktok(entity.getTiktok())
                .kitchenModuleEnabled(entity.getKitchenModuleEnabled())
                .kitchenEnabledAt(entity.getKitchenEnabledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TenantConfig toEntity(TenantConfigDTO dto, Tenant tenant) {
        TenantConfig entity = TenantConfig.builder()
                .id(dto.getId())
                .tenant(tenant)
                .history(dto.getHistory())
                .vision(dto.getVision())
                .bussinesEmail(dto.getBussinesEmail())
                .twitter(dto.getTwitter())
                .facebook(dto.getFacebook())
                .linkedin(dto.getLinkedin())
                .instagram(dto.getInstagram())
                .tiktok(dto.getTiktok())
                .kitchenModuleEnabled(dto.getKitchenModuleEnabled() != null ? dto.getKitchenModuleEnabled() : false)
                .kitchenEnabledAt(dto.getKitchenEnabledAt())
                .build();
        return entity;
    }
}
