package com.lealtixservice.controller;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.service.IWaiterDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para el dashboard del mesero.
 * Proporciona endpoints para obtener métricas y KPIs específicos del rol MESERO.
 * 
 * Rutas base: /api/dashboard/waiter
 * Permisos: Requiere rol MESERO o ADMIN con autoridad dashboard_mesero
 * 
 * SOLID Principles:
 * - Single Responsibility: Maneja solo requests/responses HTTP del dashboard mesero
 * - Dependency Inversion: Depende de IWaiterDashboardService (abstracción)
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard/waiter")
@RequiredArgsConstructor
@Validated
@Tag(name = "Waiter Dashboard", description = "Endpoints para el dashboard y métricas del mesero")
public class WaiterDashboardController {
    
    private final IWaiterDashboardService waiterDashboardService;
    
    /**
     * Obtiene el resumen del dashboard del mesero.
     * 
     * @param tenantId ID del tenant (requerido)
     * @param userId ID del usuario autenticado (requerido)
     * @return Resumen con métricas clave de hoy
     * @http 200 OK con datos del resumen
     * @http 400 Bad Request si faltan parámetros
     * @http 403 Forbidden si el usuario no tiene permisos
     */
    @Operation(summary = "Obtener resumen del dashboard",
               description = "Retorna un resumen de métricas clave del desempeño del mesero para hoy: " +
                       "ventas identificadas, nuevos clientes, órdenes, tasa de recompra")
    @GetMapping("/summary")
    //@PreAuthorize("hasAuthority('dashboard_mesero')")
    public ResponseEntity<WaiterDashboardSummaryDTO> getSummary(
            @Parameter(description = "ID del tenant/empresa", required = true)
            @RequestParam @NotNull(message = "tenantId es requerido") Long tenantId,
            
            @Parameter(description = "ID del usuario autenticado", required = true)
            @RequestParam @NotNull(message = "userId es requerido") String userId
    ) {
        log.info("GET /api/dashboard/waiter/summary - tenantId={}, userId={}", tenantId, userId);
        try {
            WaiterDashboardSummaryDTO summary = waiterDashboardService.getDashboardSummary(tenantId, userId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            log.error("Error en endpoint /summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtiene el resumen del dashboard del mesero por rango de fechas.
     * Si from y to no se proporcionan, retorna datos de hoy.
     * 
     * @param tenantId ID del tenant (requerido)
     * @param userId ID del usuario autenticado (requerido)
     * @param from Fecha inicio (formato: yyyy-MM-dd'T'HH:mm:ss) - opcional
     * @param to Fecha fin (formato: yyyy-MM-dd'T'HH:mm:ss) - opcional
     * @return Resumen con métricas del rango de fechas
     */
    @Operation(summary = "Obtener resumen del dashboard por rango de fechas",
               description = "Retorna un resumen de métricas clave del desempeño del mesero para un rango de fechas. " +
                       "Si from y to no se proporcionan, retorna datos de hoy. " +
                       "Formato de fechas: yyyy-MM-dd'T'HH:mm:ss")
    @GetMapping("/summary/by-date-range")
    //@PreAuthorize("hasAuthority('dashboard_mesero')")
    public ResponseEntity<WaiterDashboardSummaryDTO> getSummaryByDateRange(
            @Parameter(description = "ID del tenant/empresa", required = true)
            @RequestParam @NotNull(message = "tenantId es requerido") Long tenantId,
            
            @Parameter(description = "ID del usuario autenticado", required = true)
            @RequestParam @NotNull(message = "userId es requerido") String userId,
            
            @Parameter(description = "Fecha inicio (formato: yyyy-MM-dd'T'HH:mm:ss)", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            
            @Parameter(description = "Fecha fin (formato: yyyy-MM-dd'T'HH:mm:ss)", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/dashboard/waiter/summary/by-date-range - tenantId={}, userId={}, from={}, to={}", 
                tenantId, userId, from, to);
        try {
            WaiterDashboardSummaryDTO summary = waiterDashboardService.getDashboardSummaryByDateRange(tenantId, userId, from, to);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            log.error("Error en endpoint /summary/by-date-range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtiene lista de clientes VIP ordenados por LTV.
     * 
     * @param tenantId ID del tenant (requerido)
     * @param userId ID del usuario autenticado (requerido)
     * @param limit Cantidad máxima de clientes (opcional, default: 5)
     * @return Lista de clientes VIP
     * @http 200 OK con lista de clientes
     * @http 400 Bad Request si faltan parámetros
     * @http 403 Forbidden si el usuario no tiene permisos
     */
    @Operation(summary = "Obtener clientes VIP",
               description = "Retorna lista de top clientes por LTV (Life Time Value). " +
                       "Excluye clientes que han visitado en los últimos 7 días. " +
                       "Ordenados de mayor a menor LTV")
    @GetMapping("/vip-clients")
    //@PreAuthorize("hasAuthority('dashboard_mesero')")
    public ResponseEntity<List<VipClientDTO>> getVipClients(
            @Parameter(description = "ID del tenant/empresa", required = true)
            @RequestParam @NotNull(message = "tenantId es requerido") Long tenantId,
            
            @Parameter(description = "ID del usuario autenticado", required = true)
            @RequestParam @NotNull(message = "userId es requerido") String userId,
            
            @Parameter(description = "Cantidad máxima de clientes a retornar", required = false)
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("GET /api/dashboard/waiter/vip-clients - tenantId={}, userId={}, limit={}", tenantId, userId, limit);
        try {
            List<VipClientDTO> vipClients = waiterDashboardService.getVipClients(tenantId, userId, limit);
            return ResponseEntity.ok(vipClients);
        } catch (RuntimeException e) {
            log.error("Error en endpoint /vip-clients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtiene lista de productos recomendados para venta cruzada.
     * 
     * @param tenantId ID del tenant (requerido)
     * @param limit Cantidad máxima de productos (opcional, default: 10)
     * @return Lista de productos trending
     * @http 200 OK con lista de productos
     * @http 400 Bad Request si faltan parámetros
     * @http 403 Forbidden si el usuario no tiene permisos
     */
    @Operation(summary = "Obtener productos para venta cruzada",
               description = "Retorna lista de productos trending basados en las órdenes de los últimos 30 días. " +
                       "Recomendados para sugerir al cliente. Ordenados por cantidad de órdenes descendente")
    @GetMapping("/cross-sell")
    //@PreAuthorize("hasAuthority('dashboard_mesero')")
    public ResponseEntity<List<CrossSellProductDTO>> getCrossSell(
            @Parameter(description = "ID del tenant/empresa", required = true)
            @RequestParam @NotNull(message = "tenantId es requerido") Long tenantId,
            
            @Parameter(description = "Cantidad máxima de productos a retornar", required = false)
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /api/dashboard/waiter/cross-sell - tenantId={}, limit={}", tenantId, limit);
        try {
            List<CrossSellProductDTO> products = waiterDashboardService.getCrossSellSuggestions(tenantId, limit);
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            log.error("Error en endpoint /cross-sell", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtiene el dashboard completo con todas las métricas agregadas.
     * Incluye resumen, clientes VIP, productos cross-sell y mensajes.
     * 
     * @param tenantId ID del tenant (requerido)
     * @param userId ID del usuario autenticado (requerido)
     * @return Dashboard completo
     * @http 200 OK con dashboard completo
     * @http 400 Bad Request si faltan parámetros
     * @http 403 Forbidden si el usuario no tiene permisos
     */
    @Operation(summary = "Obtener dashboard completo",
               description = "Retorna un objeto agregado con: resumen de métricas, top 5 clientes VIP, " +
                       "top 10 productos trending y mensajes motivacionales. " +
                       "Ideal para cargar la UI completa del dashboard del mesero")
    @GetMapping("/complete")
    //@PreAuthorize("hasAuthority('dashboard_mesero')")
    public ResponseEntity<WaiterDashboardCompleteDTO> getComplete(
            @Parameter(description = "ID del tenant/empresa", required = true)
            @RequestParam @NotNull(message = "tenantId es requerido") Long tenantId,
            
            @Parameter(description = "ID del usuario autenticado", required = true)
            @RequestParam @NotNull(message = "userId es requerido") String userId
    ) {
        log.info("GET /api/dashboard/waiter/complete - tenantId={}, userId={}", tenantId, userId);
        try {
            WaiterDashboardCompleteDTO complete = waiterDashboardService.getCompleteDashboard(tenantId, userId);
            return ResponseEntity.ok(complete);
        } catch (RuntimeException e) {
            log.error("Error en endpoint /complete", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
