package com.lealtixservice.controller;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para endpoints de dashboard y reportes de negocio.
 * Expone KPIs de clientes, cupones, redenciones y ventas.
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints para reportes y KPIs del dashboard de negocio")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "KPI 1: Total de clientes registrados",
               description = "Obtiene el total de clientes registrados en un rango de fechas")
    @GetMapping("/customers/total")
    public ResponseEntity<Long> getTotalCustomers(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio (formato: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (formato: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/customers/total - tenantId={}, from={}, to={}", tenantId, from, to);
        Long total = dashboardService.getTotalCustomers(tenantId, from, to);
        return ResponseEntity.ok(total);
    }

    @Operation(summary = "KPI 2: Clientes nuevos por periodo",
               description = "Obtiene serie de tiempo de clientes nuevos agrupados por día/semana/mes")
    @GetMapping("/customers/new-by-period")
    public ResponseEntity<List<TimeSeriesCountDTO>> getNewCustomersByPeriod(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Periodo de agrupación: 'day', 'week', 'month'")
            @RequestParam String period,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/customers/new-by-period - tenantId={}, period={}, from={}, to={}",
                tenantId, period, from, to);
        List<TimeSeriesCountDTO> timeSeries = dashboardService.getNewCustomersByPeriod(tenantId, period, from, to);
        return ResponseEntity.ok(timeSeries);
    }

    @Operation(summary = "KPI 3: Cupones creados vs redimidos",
               description = "Obtiene estadísticas de cupones emitidos y redimidos por campaña")
    @GetMapping("/coupons/stats")
    public ResponseEntity<List<CouponStatsDTO>> getCouponStats(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/coupons/stats - tenantId={}, from={}, to={}", tenantId, from, to);
        List<CouponStatsDTO> stats = dashboardService.getCouponStats(tenantId, from, to);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "KPI 5 y 6: Resumen de ventas totales",
               description = "Obtiene total de ventas, ticket promedio y cantidad de transacciones. Incluye TODAS las órdenes: " +
                       "con o sin cliente identificado, con o sin cupón aplicado")
    @GetMapping("/sales/summary")
    public ResponseEntity<SalesSummaryDTO> getSalesSummary(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/sales/summary - tenantId={}, from={}, to={}", tenantId, from, to);
        SalesSummaryDTO summary = dashboardService.getSalesSummary(tenantId, from, to);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "KPI 7: Rendimiento por campaña",
               description = "Obtiene tabla resumen completa de performance de campañas con todas las métricas")
    @GetMapping("/campaigns/performance")
    public ResponseEntity<List<CampaignPerformanceDTO>> getCampaignPerformance(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/campaigns/performance - tenantId={}, from={}, to={}", tenantId, from, to);
        List<CampaignPerformanceDTO> performance = dashboardService.getCampaignPerformance(tenantId, from, to);
        return ResponseEntity.ok(performance);
    }

    // ==================== NUEVOS ENDPOINTS DE MÉTRICAS DE FIDELIZACIÓN (COMANDIX) ====================

    @Operation(summary = "KPI Comandix 1: Tasa de Recompra",
               description = "Calcula el porcentaje de clientes que han realizado más de una compra")
    @GetMapping("/comandix/repeat-purchase-rate")
    public ResponseEntity<RepeatPurchaseRateDTO> getRepeatPurchaseRate(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            log.info("GET /api/dashboard/comandix/repeat-purchase-rate - tenantId={}, from={}, to={}", tenantId, from, to);
            RepeatPurchaseRateDTO result = dashboardService.getRepeatPurchaseRate(tenantId, from, to);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en repeat-purchase-rate", e);
            throw e;
        }
    }

    @Operation(summary = "KPI Comandix 2: Ventas Identificadas vs Generales",
               description = "Compara ingresos y transacciones entre clientes registrados y ventas anónimas")
    @GetMapping("/comandix/identified-vs-general")
    public ResponseEntity<IdentifiedVsGeneralSalesDTO> getIdentifiedVsGeneralSales(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            log.info("GET /api/dashboard/comandix/identified-vs-general - tenantId={}, from={}, to={}", tenantId, from, to);
            log.info("Fechas recibidas - from: {} (tipo: {}), to: {} (tipo: {})", 
                    from, from != null ? from.getClass().getSimpleName() : "null",
                    to, to != null ? to.getClass().getSimpleName() : "null");
            IdentifiedVsGeneralSalesDTO result = dashboardService.getIdentifiedVsGeneralSales(tenantId, from, to);
            log.info("Resultado obtenido: {}", result);
            return ResponseEntity.ok(result);
        }catch (Exception e) {
            log.error("Error en identified-vs-general endpoint", e);
            throw e;
        }

    }

    @Operation(summary = "KPI Comandix 3: LTV (Customer Lifetime Value)",
               description = "Lista de clientes ordenados por valor total generado")
    @GetMapping("/comandix/customer-ltv")
    public ResponseEntity<List<CustomerLTVDTO>> getCustomerLTV(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Límite de resultados (top N clientes)") @RequestParam(defaultValue = "50") Integer limit
    ) {
        try {
            log.info("GET /api/dashboard/comandix/customer-ltv - tenantId={}, from={}, to={}, limit={}", tenantId, from, to, limit);
            List<CustomerLTVDTO> result = dashboardService.getCustomerLTV(tenantId, from, to, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en customer-ltv endpoint", e);
            throw e;
        }
    }

    @Operation(summary = "KPI Comandix 4: Tasa de Conversión de Cupón",
               description = "Relación entre cupones emitidos, redimidos y usados en órdenes")
    @GetMapping("/comandix/coupon-conversion")
    public ResponseEntity<List<CouponConversionRateDTO>> getCouponConversionRate(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            log.info("GET /api/dashboard/comandix/coupon-conversion - tenantId={}, from={}, to={}", tenantId, from, to);
            List<CouponConversionRateDTO> result = dashboardService.getCouponConversionRate(tenantId, from, to);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en coupon-conversion endpoint", e);
            throw e;
        }
    }

    @Operation(summary = "KPI Comandix 5: Análisis de Personalización",
               description = "Frecuencia de términos en comentarios de items para identificar tendencias de gustos")
    @GetMapping("/comandix/customization-analysis")
    public ResponseEntity<List<CustomizationAnalysisDTO>> getCustomizationAnalysis(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/comandix/customization-analysis - tenantId={}, from={}, to={}", tenantId, from, to);
        List<CustomizationAnalysisDTO> result = dashboardService.getCustomizationAnalysis(tenantId, from, to);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "KPI Comandix 6: ROI por Campaña",
               description = "Calcula retorno de inversión: (Ingresos - Costo) / Costo por cada campaña")
    @GetMapping("/comandix/campaign-roi")
    public ResponseEntity<List<CampaignROIDTO>> getCampaignROI(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            log.info("GET /api/dashboard/comandix/campaign-roi - tenantId={}, from={}, to={}", tenantId, from, to);
            List<CampaignROIDTO> result = dashboardService.getCampaignROI(tenantId, from, to);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en campaign-roi endpoint", e);
            throw e;
        }
    }

    @Operation(summary = "DEBUG: Validar datos de órdenes por rango de fechas")
    @GetMapping("/debug/orders-count")
    public ResponseEntity<?> debugOrdersCount(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Fecha inicio") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            log.info("DEBUG: Validando órdenes - tenantId={}, from={}, to={}", tenantId, from, to);
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("tenantId", tenantId);
            debug.put("from", from);
            debug.put("to", to);
            debug.put("message", "Revisa los logs para ver los conteos de órdenes");
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error en debug endpoint", e);
            throw e;
        }
    }
}