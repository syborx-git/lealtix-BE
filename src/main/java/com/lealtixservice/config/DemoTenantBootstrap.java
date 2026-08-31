package com.lealtixservice.config;

import com.lealtixservice.entity.Tenant;
import com.lealtixservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTenantBootstrap {

    private static final String DEMO_TENANT_SLUG = "demo";

    private final TenantRepository tenantRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDemoTenantExists() {
        tenantRepository.getBySlug(DEMO_TENANT_SLUG)
                .orElseGet(() -> {
                    Tenant demoTenant = Tenant.builder()
                            .nombreNegocio("Tenant Demo")
                            .direccion("Demo")
                            .telefono("0000000000")
                            .tipoNegocio("Demo")
                            .slug(DEMO_TENANT_SLUG)
                            .UIDTenant("UID-DEMO")
                            .schedules("Demo")
                            .logoUrl("")
                            .slogan("Tenant de demostracion")
                            .kitchenModuleEnabled(false)
                            .isActive(true)
                            .build();

                    Tenant savedTenant = tenantRepository.save(demoTenant);
                    log.info("Tenant demo inicializado con id={}", savedTenant.getId());
                    return savedTenant;
                });
    }
}
