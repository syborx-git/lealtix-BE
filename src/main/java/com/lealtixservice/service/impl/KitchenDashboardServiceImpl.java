package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.KitchenDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de dashboard de cocina.
 * Proporciona métricas operativas y motivacionales especializadas para equipos de cocina.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KitchenDashboardServiceImpl implements KitchenDashboardService {

    private final ClientOrderRepository clientOrderRepository;
    private final TenantRepository tenantRepository;
    private final DashboardServiceImpl dashboardService;

    @Override
    public KitchenDashboardSummaryDTO getSummary(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.info("Obteniendo resumen de dashboard cocina - tenantId={}, from={}, to={}", tenantId, from, to);

        // Normalizar fechas: si no vienen, usar hoy desde 00:00 hasta ahora
        LocalDateTime normalizedFrom = from != null ? from : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime normalizedTo = to != null ? to : LocalDateTime.now();

        try {
            // 1. Obtener nombre del tenant
            String tenantName = tenantRepository.findById(tenantId)
                    .map(t -> t.getNombreNegocio())
                    .orElse("Unknown Tenant");

            // 2. Top 3 productos más pedidos
            List<TopDishDTO> topDishes = getTopDishes(tenantId, normalizedFrom, normalizedTo);

            // 3. Tasa de recompra
            RepeatPurchaseRateDTO repeatPurchaseRate = dashboardService
                    .getRepeatPurchaseRate(tenantId, normalizedFrom, normalizedTo);

            // 4. Órdenes completadas
            CompletedOrdersDTO completedOrders = getCompletedOrders(tenantId, normalizedFrom, normalizedTo);

            // 5. Análisis de personalización
            List<CustomizationAnalysisDTO> customizationAnalysis = dashboardService
                    .getCustomizationAnalysis(tenantId, normalizedFrom, normalizedTo);

            // 6. Cliente VIP
            VIPAlertDTO vipAlert = getVIPCustomer(tenantId, normalizedFrom, normalizedTo);

            // Construir DTO final
            KitchenDashboardSummaryDTO summary = KitchenDashboardSummaryDTO.builder()
                    .tenantName(tenantName)
                    .topDishes(topDishes)
                    .repeatPurchaseRate(repeatPurchaseRate)
                    .completedOrders(completedOrders)
                    .customizationAnalysis(customizationAnalysis)
                    .vipAlert(vipAlert)
                    .build();

            log.info("Resumen de dashboard cocina obtenido exitosamente para tenantId={}", tenantId);
            return summary;

        } catch (Exception e) {
            log.error("Error al obtener resumen de dashboard cocina para tenantId={}", tenantId, e);
            throw new RuntimeException("Error obteniendo resumen del dashboard de cocina", e);
        }
    }

    /**
     * Obtiene los top 3 productos más pedidos
     */
    private List<TopDishDTO> getTopDishes(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo top 3 productos para tenantId={}", tenantId);

        List<Object[]> results = clientOrderRepository.getTopDishes(tenantId, from, to);
        List<TopDishDTO> topDishes = new ArrayList<>();

        int rank = 1;
        for (Object[] row : results) {
            Long productId = safeToLong(row[0], 0L);
            String productName = row[1] != null ? row[1].toString() : "Unknown";
            Long quantity = safeToLong(row[2], 0L);
            BigDecimal totalSales = safeToBigDecimal(row[3], BigDecimal.ZERO);

            TopDishDTO dto = TopDishDTO.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantity(quantity)
                    .totalSales(totalSales)
                    .rank(rank++)
                    .build();

            topDishes.add(dto);
        }

        return topDishes;
    }

    /**
     * Obtiene estadísticas de órdenes completadas
     */
    private CompletedOrdersDTO getCompletedOrders(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo órdenes completadas para tenantId={}", tenantId);

        Long completedCount = clientOrderRepository.countCompletedOrders(tenantId, from, to);
        Long successfulDeliveries = clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to);

        return CompletedOrdersDTO.builder()
                .completedOrders(completedCount != null ? completedCount : 0L)
                .successfulDeliveries(successfulDeliveries != null ? successfulDeliveries : 0L)
                .build();
    }

    /**
     * Obtiene información del cliente VIP (mayor LTV en el período)
     */
    private VIPAlertDTO getVIPCustomer(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo cliente VIP para tenantId={}", tenantId);

        Object[] result = clientOrderRepository.getVIPCustomer(tenantId, from, to);

        if (result == null || result.length < 4) {
            return null;
        }

        Long customerId = safeToLong(result[0], 0L);
        String customerName = result[1] != null ? result[1].toString() : "Unknown";
        String customerEmail = result[2] != null ? result[2].toString() : "";
        BigDecimal lifetimeValue = safeToBigDecimal(result[3], BigDecimal.ZERO);

        // VIP si LTV > 0
        if (lifetimeValue.compareTo(BigDecimal.ZERO) > 0) {
            return VIPAlertDTO.builder()
                    .active(true)
                    .customerId(customerId)
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .lifetimeValue(lifetimeValue)
                    .note("Top customer by lifetime value")
                    .build();
        }

        return null;
    }

    /**
     * Método auxiliar para convertir un Object a Number de forma segura
     */
    private Long safeToLong(Object obj, Long defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("No se pudo convertir {} a Long, usando valor por defecto {}", obj, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Método auxiliar para convertir un Object a BigDecimal de forma segura
     */
    private BigDecimal safeToBigDecimal(Object obj, BigDecimal defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("No se pudo convertir {} a BigDecimal, usando valor por defecto {}", obj, defaultValue);
            return defaultValue;
        }
    }
}
