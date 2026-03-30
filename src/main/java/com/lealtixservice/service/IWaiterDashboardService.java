package com.lealtixservice.service;

import com.lealtixservice.dto.dashboard.*;

import java.util.List;

/**
 * Interfaz de servicio para el dashboard del mesero.
 * Define los contratos de negocio para obtener métricas y datos agregados.
 * 
 * SOLID Principles:
 * - Single Responsibility: Responsabilidad única en obtener datos del dashboard mesero
 * - Open/Closed: Abierto para extensión, cerrado para modificación
 * - Dependency Inversion: Depende de abstracciones, no de implementaciones concretas
 */
public interface IWaiterDashboardService {
    
    /**
     * Obtiene el resumen del dashboard del mesero para hoy.
     * Contiene métricas clave de desempeño: ventas, clientes nuevos, órdenes, tasa recompra.
     * 
     * @param tenantId ID del tenant/empresa
     * @param userId ID del usuario autenticado
     * @return DTO con resumen de métricas
     */
    WaiterDashboardSummaryDTO getDashboardSummary(Long tenantId, String userId);
    
    /**
     * Obtiene el resumen del dashboard del mesero para un rango de fechas.
     * Contiene métricas clave de desempeño: ventas, clientes nuevos, órdenes, tasa recompra.
     * Si from y to son null, usa el día actual.
     * 
     * @param tenantId ID del tenant/empresa
     * @param userId ID del usuario autenticado
     * @param from Fecha inicio (yyyy-MM-dd'T'HH:mm:ss) - opcional
     * @param to Fecha fin (yyyy-MM-dd'T'HH:mm:ss) - opcional
     * @return DTO con resumen de métricas
     */
    WaiterDashboardSummaryDTO getDashboardSummaryByDateRange(Long tenantId, String userId, 
                                                              java.time.LocalDateTime from, 
                                                              java.time.LocalDateTime to);
    
    /**
     * Obtiene lista de clientes VIP ordenados por LTV (Life Time Value).
     * Excluye clientes que visitaron en los últimos 7 días.
     * 
     * @param tenantId ID del tenant/empresa
     * @param userId ID del usuario autenticado
     * @param limit Cantidad máxima de clientes a retornar (default: 5)
     * @return Lista de clientes VIP con sus métricas
     */
    List<VipClientDTO> getVipClients(Long tenantId, String userId, int limit);
    
    /**
     * Obtiene lista de productos recomendados para venta cruzada.
     * Se basan en los más trendy (más ordenados en últimos 30 días).
     * 
     * @param tenantId ID del tenant/empresa
     * @param limit Cantidad máxima de productos a retornar (default: 10)
     * @return Lista de productos para cross-sell
     */
    List<CrossSellProductDTO> getCrossSellSuggestions(Long tenantId, int limit);
    
    /**
     * Obtiene el dashboard completo del mesero con todas las métricas.
     * Agregación de resumen, clientes VIP, productos y mensajes.
     * 
     * @param tenantId ID del tenant/empresa
     * @param userId ID del usuario autenticado
     * @return DTO completo del dashboard
     */
    WaiterDashboardCompleteDTO getCompleteDashboard(Long tenantId, String userId);
}