package com.lealtixservice.service;

import com.lealtixservice.dto.TenantDTO;
import com.lealtixservice.dto.TenantWizardDTO;
import com.lealtixservice.entity.Tenant;
import java.util.List;
import java.util.Optional;

public interface TenantService {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    List<Tenant> findAll();
    void deleteById(Long id);

    Optional<Tenant> updateTenant(Long id, TenantDTO dto);

    Tenant create(TenantDTO tenant);

    TenantWizardDTO getBySlug(String slug);

    TenantDTO getByEmail(String email);

    void saveCustomSite(Long id, String html);
}

