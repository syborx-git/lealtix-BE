package com.lealtixservice.service.impl;

import com.lealtixservice.dto.TenantDTO;
import com.lealtixservice.dto.TenantWizardDTO;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantConfig;
import com.lealtixservice.repository.TenantConfigRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.AppUserService;
import com.lealtixservice.service.TenantService;
import com.lealtixservice.util.StringUtils;
import com.lealtixservice.util.TenantUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TenantServiceImpl implements TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    @Autowired
    private AppUserService appUserService;

    @Override
    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return tenantRepository.findById(id);
    }

    @Override
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        tenantRepository.deleteById(id);
    }

    @Override
    public Optional<Tenant> updateTenant(Long id, TenantDTO dto) {
        if(id != null && dto != null) {
            return tenantRepository.findById(id).map(existingTenant -> {
                // Actualiza solo si el DTO trae valor, si no, deja el valor actual
                if (dto.getNombreNegocio() != null && !dto.getNombreNegocio().isBlank()) {
                    existingTenant.setNombreNegocio(dto.getNombreNegocio());
                }
                if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
                    existingTenant.setDireccion(dto.getDireccion());
                }
                if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
                    existingTenant.setTelefono(dto.getTelefono());
                }
                if (dto.getTipoNegocio() != null && !dto.getTipoNegocio().isBlank()) {
                    existingTenant.setTipoNegocio(dto.getTipoNegocio());
                }
                if (dto.getSlogan() != null && !dto.getSlogan().isBlank()) {
                    existingTenant.setSlogan(dto.getSlogan());
                } else if (existingTenant.getSlogan() == null) {
                    existingTenant.setSlogan(""); // Valor por defecto
                }
                if (dto.getLogoUrl() != null && !dto.getLogoUrl().isBlank()) {
                    String logoUrlDecoded = StringUtils.decryptBase64(dto.getLogoUrl());
                    existingTenant.setLogoUrl(logoUrlDecoded);
                } else if (existingTenant.getLogoUrl() == null) {
                    existingTenant.setLogoUrl(""); // Valor por defecto
                }
                if (existingTenant.getSlug() == null || existingTenant.getSlug().isBlank()) {
                    existingTenant.setSlug(StringUtils.createSlug(dto.getNombreNegocio() != null ? dto.getNombreNegocio() : existingTenant.getNombreNegocio(), id));
                }
                if (dto.getUIDTenant() != null && !dto.getUIDTenant().isBlank()) {
                    existingTenant.setUIDTenant(dto.getUIDTenant());
                } else if (existingTenant.getUIDTenant() == null) {
                    existingTenant.setUIDTenant("UID-" + id);
                }
                if (existingTenant.getCreatedAt() == null) {
                    existingTenant.setCreatedAt(LocalDateTime.now());
                }
                if (dto.getKitchenModuleEnabled() != null) {
                    existingTenant.setKitchenModuleEnabled(dto.getKitchenModuleEnabled());
                    if (dto.getKitchenModuleEnabled() && existingTenant.getKitchenEnabledAt() == null) {
                        existingTenant.setKitchenEnabledAt(LocalDateTime.now());
                    }
                }
                existingTenant.setUpdatedAt(LocalDateTime.now());
                existingTenant.setActive(true);
                return tenantRepository.save(existingTenant);
            });
        }
        return Optional.empty();
    }

    @Override
    public Tenant create(TenantDTO tenantDto) {
                    if (tenantDto == null) {
                        return null;
                    }

                    Tenant tenant = new Tenant();
                    if(tenantDto.getId() != null && tenantDto.getId() > 0){
                        tenant = tenantRepository.findById(tenantDto.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + tenantDto.getId()));
                    }else{
                        AppUser user = appUserService.findById(tenantDto.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + tenantDto.getUserId()));
                        tenant.setCreatedAt(LocalDateTime.now());
                        tenant.setAppUser(user);
                        tenant.setNombreNegocio(tenantDto.getNombreNegocio());
                        tenant.setLogoUrl(tenantDto.getLogoUrl());
                        tenant.setSlogan(tenantDto.getSlogan());
                        tenant.setTipoNegocio("Cafeteria");
                    }

                    // Important: persistir el tenant primero si es nuevo para que tenga un ID
                    if (tenant.getId() == null) {
                        tenant = tenantRepository.save(tenant);
                    }

                    TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(tenant.getId());
                    if (tenantConfig == null) {
                        tenantConfig = new TenantConfig();
                        tenantConfig.setTenant(tenant);
                        tenantConfig.setCreatedAt(LocalDateTime.now());
                    }
                    tenantConfig.setUpdatedAt(LocalDateTime.now());
                    tenantConfig.setHistory(tenantDto.getHistory());
                    tenantConfig.setVision(tenantDto.getVision());
                    tenantConfig.setBussinesEmail(tenantDto.getBussinessEmail());
                    // social media
                    tenantConfig.setFacebook(tenantDto.getFacebook());
                    tenantConfig.setInstagram(tenantDto.getInstagram());
                    tenantConfig.setTiktok(tenantDto.getTiktok());
                    tenantConfig.setTwitter(tenantDto.getX());
                    tenantConfig.setLinkedin(tenantDto.getLinkedin());
                    tenantConfigRepository.save(tenantConfig);

                    // Actualiza campos del tenant con la información del DTO
                    tenant.setNombreNegocio(tenantDto.getNombreNegocio());
                    // Decodifica logo si viene en base64, si no, lo deja tal cual
                    if (tenantDto.getLogoUrl() != null && !tenantDto.getLogoUrl().isBlank()) {
                        String logoUrlDecoded = StringUtils.decryptBase64(tenantDto.getLogoUrl());
                        tenant.setLogoUrl(logoUrlDecoded);
                    } else {
                        tenant.setLogoUrl("");
                    }
                    tenant.setDireccion(tenantDto.getDireccion());
                    tenant.setTelefono(tenantDto.getTelefono());
                    tenant.setTipoNegocio(tenantDto.getTipoNegocio());
                    tenant.setUpdatedAt(LocalDateTime.now());
                    tenant.setSchedules(tenantDto.getSchedules());
                    tenant.setTipoNegocio("Cafeteria");
                    tenant.setSlogan(tenantDto.getSlogan());

                    // Garantizar que slug y UIDTenant estén creados usando el ID persistido
                    Long tenantId = tenant.getId();
                    if (tenantId != null) {
                        // Determinar nombre a usar para el slug
                        String nombreParaSlug = (tenantDto.getNombreNegocio() != null && !tenantDto.getNombreNegocio().isBlank())
                                ? tenantDto.getNombreNegocio()
                                : tenant.getNombreNegocio();
                        if (tenant.getSlug() == null || tenant.getSlug().isBlank()) {
                            String generatedSlug = StringUtils.createSlug(
                                    nombreParaSlug != null ? nombreParaSlug : "tenant", tenantId);
                            tenant.setSlug(generatedSlug);
                        }

                        if (tenantDto.getUIDTenant() != null && !tenantDto.getUIDTenant().isBlank()) {
                            tenant.setUIDTenant(tenantDto.getUIDTenant());
                        } else if (tenant.getUIDTenant() == null || tenant.getUIDTenant().isBlank()) {
                            tenant.setUIDTenant("UID-" + tenantId);
                        }
                    }

                    // Validar y configurar kitchenModuleEnabled
                    if (tenantDto.getKitchenModuleEnabled() != null) {
                        tenant.setKitchenModuleEnabled(tenantDto.getKitchenModuleEnabled());
                        if (tenantDto.getKitchenModuleEnabled()) {
                            tenant.setKitchenEnabledAt(LocalDateTime.now());
                        }
                    }

                    return tenantRepository.save(tenant);
                }

    @Override
    public TenantWizardDTO getBySlug(String slug) {
        Tenant tenant = tenantRepository.getBySlug(slug).orElse(null);
        if (tenant == null) {
            return null;
        }

        TenantWizardDTO resp = new TenantWizardDTO();
        resp.setTenant(TenantUserMapper.toTenantDTO(tenant));

        if (tenant.getAppUser() != null) {
            resp.setUser(TenantUserMapper.toAppUserDTO(tenant.getAppUser()));
        }

        TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(tenant.getId());
        if (tenantConfig != null) {
            resp.setTenantConfig(TenantUserMapper.toTenantConfigDTO(tenantConfig));
        }

        return resp;
    }

    @Override
    public TenantDTO getByEmail(String email) {

        TenantDTO result = null;
        AppUser user = appUserService.findByEmail(email).orElse(null);
        if (user != null) {
            Tenant tenant = tenantRepository.findByAppUserId(user.getId()).orElse(null);
            if (tenant != null) {
                result = TenantUserMapper.toTenantDTO(tenant);
                TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(tenant.getId());
                if (tenantConfig != null) {
                    result = TenantUserMapper.toTenantDTOWithConfig(tenant, tenantConfig);
                }
                result.setUserId(user.getId());
                result.setEmail(email);
                return result;
            }
        }
        return null;
    }
}
