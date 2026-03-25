package com.lealtixservice.service;

import com.lealtixservice.dto.dashboard.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para generar reportes y KPIs del dashboard de negocio.
 */
public interface DashboardService {

    /**
     * KPI 1: Total de clientes registrados por tenant y rango de fechas.
     */
    Long getTotalCustomers(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI 2: Clientes nuevos por periodo (día/semana/mes).
     * @param period 'day', 'week', 'month'
     */
    List<TimeSeriesCountDTO> getNewCustomersByPeriod(
            Long tenantId,
            String period,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * KPI 3: Cupones creados vs cupones redimidos.
     */
    List<CouponStatsDTO> getCouponStats(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI 5: Ventas totales generadas por cupones.
     * KPI 6: Ticket promedio por cupón redimido.
     */
    SalesSummaryDTO getSalesSummary(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI 7: Rendimiento por campaña (tabla resumen completa).
     */
    List<CampaignPerformanceDTO> getCampaignPerformance(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to
    );

    // ==================== NUEVOS KPIs DE FIDELIZACIÓN Y COMANDIX ====================

    /**
     * KPI COMANDIX 1: Tasa de Recompra (Repeat Purchase Rate)
     * Calcula el porcentaje de clientes que han realizado más de una compra.
     */
    RepeatPurchaseRateDTO getRepeatPurchaseRate(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI COMANDIX 2: Ventas Identificadas vs Generales
     * Compara ingresos y transacciones entre clientes registrados y ventas anónimas.
     */
    IdentifiedVsGeneralSalesDTO getIdentifiedVsGeneralSales(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI COMANDIX 3: LTV (Customer Lifetime Value)
     * Lista de clientes ordenados por valor total generado.
     */
    List<CustomerLTVDTO> getCustomerLTV(Long tenantId, LocalDateTime from, LocalDateTime to, Integer limit);

    /**
     * KPI COMANDIX 4: Tasa de Conversión de Cupón
     * Relación entre cupones emitidos, redimidos y usados en órdenes.
     */
    List<CouponConversionRateDTO> getCouponConversionRate(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI COMANDIX 5: Análisis de Personalización
     * Frecuencia de términos en comentarios de items para identificar tendencias.
     */
    List<CustomizationAnalysisDTO> getCustomizationAnalysis(Long tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * KPI COMANDIX 6: ROI por Campaña
     * Calcula retorno de inversión: (Ingresos - Costo) / Costo
     */
    List<CampaignROIDTO> getCampaignROI(Long tenantId, LocalDateTime from, LocalDateTime to);

}