package com.lealtixservice.service;

import com.lealtixservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Job de respaldo que mantiene consistentes los productos del menú según el stock:
 * desactiva los que ya no pueden prepararse y reactiva los que vuelven a tener insumos.
 * Es una red de seguridad; la sincronización principal ocurre de forma reactiva en cada
 * operación de stock/recetas/vendas.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductAvailabilityScheduler {

    private final InventoryService inventoryService;
    private final TenantRepository tenantRepository;

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${product.availability.sync.interval:300000}")
    public void syncAllTenants() {
        try {
            List<Long> tenantIds = tenantRepository.findAllTenantIds();
            for (Long tenantId : tenantIds) {
                int changes = inventoryService.syncProductAvailabilityByTenant(tenantId);
                if (changes > 0) {
                    log.info("[AutoDisponibilidad] Job: {} producto(s) ajustados en tenant {}", changes, tenantId);
                }
            }
        } catch (Exception e) {
            log.error("[AutoDisponibilidad] Error en el job de sincronización: {}", e.getMessage(), e);
        }
    }
}