package com.lealtixservice.service;

import com.lealtixservice.dto.dashboard.KitchenDashboardSummaryDTO;
import java.time.LocalDateTime;

/**
 * Servicio especializado para el dashboard de cocina.
 * Proporciona métricas operativas y motivacionales para equipos de cocina.
 */
public interface KitchenDashboardService {

    /**
     * Obtiene resumen completo del dashboard de cocina.
     * 
     * @param tenantId ID del tenant
     * @param from fecha inicio (ISO-8601). Si es null, se usa hoy a las 00:00:00
     * @param to fecha fin (ISO-8601). Si es null, se usa ahora
     * @return KitchenDashboardSummaryDTO con todas las métricas
     */
    KitchenDashboardSummaryDTO getSummary(Long tenantId, LocalDateTime from, LocalDateTime to);
}
