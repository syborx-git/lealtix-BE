package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.WaiterDashboardRepository;
import com.lealtixservice.service.IWaiterDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de dashboard del mesero.
 * 
 * SOLID Principles:
 * - Single Responsibility: Concentra la lógica de negocio del dashboard mesero
 * - Dependency Injection: Inyecta repositorios mediante constructor
 * - Transactional: Operaciones de lectura optimizadas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaiterDashboardServiceImpl implements IWaiterDashboardService {
    
    private final WaiterDashboardRepository waiterDashboardRepository;
    
    /**
     * Obtiene el resumen del dashboard del mesero para hoy.
     * 
     * Lógica:
     * 1. Calcula porcentaje de ventas identificadas
     * 2. Cuenta nuevos clientes registrados hoy
     * 3. Cuenta órdenes procesadas hoy
     * 4. Calcula tasa de recompra
     * 5. Obtiene totales de ventas identificadas y generales
     * 
     * @param tenantId ID del tenant
     * @param userId ID del usuario (usado para auditoría/logs)
     * @return DTO con resumen de métricas
     */
    @Override
    @Transactional(readOnly = true)
    public WaiterDashboardSummaryDTO getDashboardSummary(Long tenantId, String userId) {
        log.info("Obteniendo resumen dashboard para tenant={}, userId={}", tenantId, userId);
        
        try {
            // Porcentaje de ventas identificadas
            Double salesIdentifiedPercentage = waiterDashboardRepository.calculateSalesIdentifiedPercentage(tenantId);
            if (salesIdentifiedPercentage == null) {
                salesIdentifiedPercentage = 0.0;
            }
            
            // Nuevos clientes hoy
            Long newClientsToday = waiterDashboardRepository.countNewClientsToday(tenantId);
            if (newClientsToday == null) {
                newClientsToday = 0L;
            }
            
            // Órdenes procesadas hoy
            Long ordersToday = waiterDashboardRepository.countOrdersForToday(tenantId);
            if (ordersToday == null) {
                ordersToday = 0L;
            }
            
            // Tasa de recompra
            Double repurchaseRate = waiterDashboardRepository.calculateRepurchaseRate(tenantId);
            if (repurchaseRate == null) {
                repurchaseRate = 0.0;
            }
            
            // Totales de ventas
            Object[] identifiedSales = waiterDashboardRepository.getTotalIdentifiedSalesForToday(tenantId);
            BigDecimal totalSalesIdentified = identifiedSales != null && identifiedSales[0] != null 
                ? new BigDecimal(identifiedSales[0].toString()) 
                : BigDecimal.ZERO;
            
            Object[] generalSales = waiterDashboardRepository.getTotalGeneralSalesForToday(tenantId);
            BigDecimal totalSalesGeneral = generalSales != null && generalSales[0] != null 
                ? new BigDecimal(generalSales[0].toString()) 
                : BigDecimal.ZERO;
            
            log.debug("Dashboard summary obtenido: identified%={}, newClients={}, orders={}, repurchase%={}",
                    salesIdentifiedPercentage, newClientsToday, ordersToday, repurchaseRate);
            
            return WaiterDashboardSummaryDTO.builder()
                    .salesIdentifiedPercentage(Math.round(salesIdentifiedPercentage * 100.0) / 100.0)
                    .newClientsToday(newClientsToday)
                    .ordersToday(ordersToday)
                    .repurchaseRate(Math.round(repurchaseRate * 100.0) / 100.0)
                    .totalSalesIdentified(totalSalesIdentified)
                    .totalSalesGeneral(totalSalesGeneral)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo resumen del dashboard para tenant={}", tenantId, e);
            throw new RuntimeException("No se pudo obtener resumen del dashboard", e);
        }
    }
    
    /**
     * Obtiene el resumen del dashboard por rango de fechas.
     * Si from y to son null, delega al método getDashboardSummary (hoy).
     * 
     * @param tenantId ID del tenant
     * @param userId ID del usuario
     * @param from Fecha inicio (opcional)
     * @param to Fecha fin (opcional)
     * @return DTO con resumen de métricas
     */
    @Override
    @Transactional(readOnly = true)
    public WaiterDashboardSummaryDTO getDashboardSummaryByDateRange(Long tenantId, String userId, 
                                                                      LocalDateTime from, LocalDateTime to) {
        log.info("Obteniendo resumen dashboard por fechas para tenant={}, userId={}, from={}, to={}", 
                tenantId, userId, from, to);
        
        // Si no hay fechas, usar el método para hoy
        if (from == null || to == null) {
            return getDashboardSummary(tenantId, userId);
        }
        
        try {
            // Porcentaje de ventas identificadas
            Double salesIdentifiedPercentage = waiterDashboardRepository.calculateSalesIdentifiedPercentageByDateRange(tenantId, from, to);
            if (salesIdentifiedPercentage == null) {
                salesIdentifiedPercentage = 0.0;
            }
            
            // Nuevos clientes en el rango
            Long newClientsInRange = waiterDashboardRepository.countNewClientsByDateRange(tenantId, from, to);
            if (newClientsInRange == null) {
                newClientsInRange = 0L;
            }
            
            // Órdenes en el rango
            Long ordersInRange = waiterDashboardRepository.countOrdersByDateRange(tenantId, from, to);
            if (ordersInRange == null) {
                ordersInRange = 0L;
            }
            
            // Tasa de recompra en el rango
            Double repurchaseRate = waiterDashboardRepository.calculateRepurchaseRateByDateRange(tenantId, from, to);
            if (repurchaseRate == null) {
                repurchaseRate = 0.0;
            }
            
            // Totales de ventas en el rango
            Object[] identifiedSales = waiterDashboardRepository.getTotalIdentifiedSalesByDateRange(tenantId, from, to);
            BigDecimal totalSalesIdentified = identifiedSales != null && identifiedSales[0] != null 
                ? new BigDecimal(identifiedSales[0].toString()) 
                : BigDecimal.ZERO;
            
            Object[] generalSales = waiterDashboardRepository.getTotalGeneralSalesByDateRange(tenantId, from, to);
            BigDecimal totalSalesGeneral = generalSales != null && generalSales[0] != null 
                ? new BigDecimal(generalSales[0].toString()) 
                : BigDecimal.ZERO;
            
            log.debug("Dashboard summary obtenido por rango: identified%={}, newClients={}, orders={}, repurchase%={}",
                    salesIdentifiedPercentage, newClientsInRange, ordersInRange, repurchaseRate);
            
            return WaiterDashboardSummaryDTO.builder()
                    .salesIdentifiedPercentage(Math.round(salesIdentifiedPercentage * 100.0) / 100.0)
                    .newClientsToday(newClientsInRange)
                    .ordersToday(ordersInRange)
                    .repurchaseRate(Math.round(repurchaseRate * 100.0) / 100.0)
                    .totalSalesIdentified(totalSalesIdentified)
                    .totalSalesGeneral(totalSalesGeneral)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo resumen del dashboard por fechas para tenant={}", tenantId, e);
            throw new RuntimeException("No se pudo obtener resumen del dashboard por rango de fechas", e);
        }
    }
    
    /**
     * Obtiene lista de clientes VIP ordenados por LTV.
     * Excluye clientes que visitaron en los últimos 7 días.
     * 
     * @param tenantId ID del tenant
     * @param userId ID del usuario
     * @param limit Cantidad máxima de clientes a retornar
     * @return Lista de clientes VIP
     */
    @Override
    @Transactional(readOnly = true)
    public List<VipClientDTO> getVipClients(Long tenantId, String userId, int limit) {
        log.info("Obteniendo clientes VIP para tenant={}, userId={}, limit={}", tenantId, userId, limit);
        
        try {
            List<Object[]> rawData = waiterDashboardRepository.getVipClients(tenantId, limit);
            
            return rawData.stream()
                    .map(row -> VipClientDTO.builder()
                            .id((Long) row[0])
                            .name((String) row[1])
                            .email((String) row[2])
                            .phone((String) row[3])
                            .ltv(new BigDecimal(row[4].toString()))
                            .lastVisitDate((LocalDateTime) row[5])
                            .visitCount((Long) row[6])
                            .averageTicket(new BigDecimal(row[7].toString()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error obteniendo clientes VIP para tenant={}", tenantId, e);
            throw new RuntimeException("No se pudieron obtener clientes VIP", e);
        }
    }
    
    /**
     * Obtiene lista de productos recomendados para venta cruzada.
     * Basados en los más ordenados en los últimos 30 días.
     * 
     * @param tenantId ID del tenant
     * @param limit Cantidad máxima de productos a retornar
     * @return Lista de productos para cross-sell
     */
    @Override
    @Transactional(readOnly = true)
    public List<CrossSellProductDTO> getCrossSellSuggestions(Long tenantId, int limit) {
        log.info("Obteniendo productos cross-sell para tenant={}, limit={}", tenantId, limit);
        
        try {
            List<Object[]> rawData = waiterDashboardRepository.getCrossSellProducts(tenantId, limit);
            
            return rawData.stream()
                    .map(row -> CrossSellProductDTO.builder()
                            .id((Long) row[0])
                            .name((String) row[1])
                            .price(new BigDecimal(row[2].toString()))
                            .imageUrl((String) row[3])
                            .category((String) row[4])
                            .suggestedFor("MESERO")
                            .stock(0L) // Stock no disponible en query actual, colocar 0
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error obteniendo productos cross-sell para tenant={}", tenantId, e);
            throw new RuntimeException("No se pudieron obtener sugerencias de venta cruzada", e);
        }
    }
    /**
     * Obtiene el dashboard completo agregado con todas las métricas.
     * 
     * @param tenantId ID del tenant
     * @param userId ID del usuario
     * @return DTO completo del dashboard
     */
    @Override
    @Transactional(readOnly = true)
    public WaiterDashboardCompleteDTO getCompleteDashboard(Long tenantId, String userId) {
        log.info("Obteniendo dashboard completo para tenant={}, userId={}", tenantId, userId);
        
        try {
            // Obtener todos los componentes
            WaiterDashboardSummaryDTO summary = getDashboardSummary(tenantId, userId);
            List<VipClientDTO> vipClients = getVipClients(tenantId, userId, 5);
            List<CrossSellProductDTO> crossSellProducts = getCrossSellSuggestions(tenantId, 10);
            
            // Generar mensajes motivacionales basados en métricas
            List<MessageDTO> messages = generateMessages(summary);
            
            return WaiterDashboardCompleteDTO.builder()
                    .summary(summary)
                    .vipClients(vipClients)
                    .crossSellProducts(crossSellProducts)
                    .messages(messages)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo dashboard completo para tenant={}", tenantId, e);
            throw new RuntimeException("No se pudo obtener el dashboard completo", e);
        }
    }
    
    /**
     * Genera mensajes/alertas motivacionales basados en métricas.
     * Utiliza los datos del resumen para crear mensajes contextualizados.
     * 
     * @param summary DTO con métricas del dashboard
     * @return Lista de mensajes generados
     */
    private List<MessageDTO> generateMessages(WaiterDashboardSummaryDTO summary) {
        List<MessageDTO> messages = new ArrayList<>();
        
        if (summary.getOrdersToday() > 20) {
            messages.add(MessageDTO.builder()
                    .type("success")
                    .text("¡Increíble desempeño hoy!")
                    .build());
        }
        
        if (summary.getRepurchaseRate() > 50) {
            messages.add(MessageDTO.builder()
                    .type("success")
                    .text("¡Excelente tasa de recompra!")
                    .build());
        }
        
        if (summary.getSalesIdentifiedPercentage() > 80) {
            messages.add(MessageDTO.builder()
                    .type("info")
                    .text("Mantén el ritmo identificando a más clientes")
                    .build());
        }
        
        if (summary.getNewClientsToday() > 10) {
            messages.add(MessageDTO.builder()
                    .type("success")
                    .text("Muchos clientes nuevos hoy")
                    .build());
        }
        
        if (messages.isEmpty()) {
            messages.add(MessageDTO.builder()
                    .type("info")
                    .text("Sigue trabajando y mejorando")
                    .build());
        }
        
        return messages;
    }
}