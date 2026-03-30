package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.DashboardCustomerRepository;
import com.lealtixservice.repository.DashboardRedemptionRepository;
import com.lealtixservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de dashboard con queries optimizadas para reportes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardCustomerRepository customerRepository;
    private final DashboardRedemptionRepository redemptionRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;

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

    /**
     * Método auxiliar para convertir un Object a Double de forma segura
     */
    private Double safeToDouble(Object obj, Double defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("No se pudo convertir {} a Double, usando valor por defecto {}", obj, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Método auxiliar para normalizar rango de fechas (ajusta a medianoche)
     */
    private LocalDateTime normalizeStartDate(LocalDateTime date) {
        // Asegurar que la fecha inicial comience a las 00:00:00 del día
        return date.toLocalDate().atStartOfDay();
    }

    private LocalDateTime normalizeEndDate(LocalDateTime date) {
        // Asegurar que la fecha final termine a las 23:59:59 del día
        return date.toLocalDate().plusDays(1).atStartOfDay().minusSeconds(1);
    }

    @Override
    public Long getTotalCustomers(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo total de clientes para tenant {} entre {} y {}", tenantId, from, to);
        return customerRepository.countByTenantAndDateRange(tenantId, from, to);
    }

    @Override
    public List<TimeSeriesCountDTO> getNewCustomersByPeriod(
            Long tenantId,
            String period,
            LocalDateTime from,
            LocalDateTime to
    ) {
        log.debug("Obteniendo clientes nuevos por {} para tenant {} entre {} y {}",
                period, tenantId, from, to);

        // Validar periodo
        if (!List.of("day", "week", "month").contains(period.toLowerCase())) {
            throw new IllegalArgumentException("Period debe ser 'day', 'week' o 'month'");
        }

        List<Object[]> results = customerRepository.findNewCustomersByPeriod(
                tenantId,
                period.toLowerCase(),
                from,
                to
        );

        return results.stream()
                .map(row -> new TimeSeriesCountDTO(
                        row[0] != null ? ((Date) row[0]).toLocalDate() : null,
                        safeToLong(row[1], 0L)
                ))
                .toList();
    }

    @Override
    public List<CouponStatsDTO> getCouponStats(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo estadísticas de cupones para tenant {} entre {} y {}", tenantId, from, to);

        List<Object[]> results = redemptionRepository.findCouponStatsByCampaign(tenantId, from, to);

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .map(row -> {
                    Long campaignId = safeToLong(row[0], null);
                    String campaignName = (String) row[1];
                    Long couponsCreated = safeToLong(row[2], 0L);
                    Long couponsRedeemed = safeToLong(row[3], 0L);
                    Double redemptionRatePct = safeToDouble(row[4], 0.0);

                    return new CouponStatsDTO(campaignId, campaignName, couponsCreated, couponsRedeemed, redemptionRatePct);
                })
                .toList();
    }

    @Override
    public SalesSummaryDTO getSalesSummary(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.info("Obteniendo resumen de ventas para tenant {} entre {} y {}", tenantId, from, to);

        // Obtener estadísticas de TODAS las órdenes (con o sin cliente, con o sin cupón)
        Object[] totalStats = clientOrderRepository.getSalesSummary(tenantId, from, to);
        
        // Obtener estadísticas de órdenes con cupón
        Object[] withCouponStats = clientOrderRepository.getSalesSummaryWithCoupon(tenantId, from, to);
        
        // Obtener estadísticas de órdenes sin cupón
        Object[] withoutCouponStats = clientOrderRepository.getSalesSummaryWithoutCoupon(tenantId, from, to);

        log.info("Total Stats: {}", totalStats != null ? Arrays.toString(totalStats) : "null");
        log.info("With Coupon Stats: {}", withCouponStats != null ? Arrays.toString(withCouponStats) : "null");
        log.info("Without Coupon Stats: {}", withoutCouponStats != null ? Arrays.toString(withoutCouponStats) : "null");

        // Manejar nulls
        if (totalStats == null) {
            totalStats = new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L};
        }

        BigDecimal totalSales = safeToBigDecimal(totalStats[0], BigDecimal.ZERO);
        BigDecimal avgTicket = safeToBigDecimal(totalStats[1], BigDecimal.ZERO);
        Long transactionCount = safeToLong(totalStats[2], 0L);

        log.info("Resumen de ventas - Total: {}, Promedio: {}, Transacciones: {}", 
                totalSales, avgTicket, transactionCount);

        return new SalesSummaryDTO(totalSales, avgTicket, transactionCount);
    }

    @Override
    public List<CampaignPerformanceDTO> getCampaignPerformance(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        log.debug("Obteniendo rendimiento de campañas para tenant {} entre {} y {}",
                tenantId, from, to);

        List<Object[]> results = redemptionRepository.findCampaignPerformance(tenantId, from, to);

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .map(row -> {
                    Long campaignId = safeToLong(row[0], null);
                    String campaignName = (String) row[1];
                    Long couponsIssued = safeToLong(row[2], 0L);
                    Long redemptions = safeToLong(row[3], 0L);
                    BigDecimal totalSales = safeToBigDecimal(row[4], BigDecimal.ZERO);
                    BigDecimal avgTicket = safeToBigDecimal(row[5], BigDecimal.ZERO);
                    Double redemptionRatePct = safeToDouble(row[6], 0.0);

                    return new CampaignPerformanceDTO(
                            campaignId,
                            campaignName,
                            couponsIssued,
                            redemptions,
                            totalSales,
                            avgTicket,
                            redemptionRatePct
                    );
                })
                .toList();
    }

    // ==================== IMPLEMENTACIÓN DE NUEVOS KPIs DE FIDELIZACIÓN ====================

    @Override
    public RepeatPurchaseRateDTO getRepeatPurchaseRate(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Calculando tasa de recompra para tenant {} entre {} y {}", tenantId, from, to);

        Long totalCustomers = clientOrderRepository.countUniqueCustomersWithOrders(tenantId, from, to);
        Long repeatCustomers = clientOrderRepository.countRepeatCustomers(tenantId, from, to);

        if (totalCustomers == null) totalCustomers = 0L;
        if (repeatCustomers == null) repeatCustomers = 0L;

        Long oneTimeBuyers = totalCustomers - repeatCustomers;
        BigDecimal repeatRate = totalCustomers > 0
                ? BigDecimal.valueOf(repeatCustomers * 100.0 / totalCustomers).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RepeatPurchaseRateDTO.builder()
                .totalCustomers(totalCustomers)
                .repeatCustomers(repeatCustomers)
                .repeatRate(repeatRate)
                .oneTimeBuyers(oneTimeBuyers)
                .multiTimeBuyers(repeatCustomers)
                .build();
    }

    @Override
    public IdentifiedVsGeneralSalesDTO getIdentifiedVsGeneralSales(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.info("Comparando ventas identificadas vs generales para tenant {} entre {} y {}", tenantId, from, to);
        log.info("Fechas recibidas - from: {}, to: {}", from, to);

        // Debug: Información sobre fechas en la BD
        Object[] dateRange = clientOrderRepository.getDateRangeInDB(tenantId);
        log.info("Rango de fechas en BD: min={}, max={}", 
                dateRange != null && dateRange.length > 0 ? dateRange[0] : "N/A",
                dateRange != null && dateRange.length > 1 ? dateRange[1] : "N/A");

        // Debug: Contar órdenes totales
        Long totalOrdersInDB = clientOrderRepository.countAllOrdersByTenant(tenantId);
        log.info("Total órdenes en BD para tenant {}: {}", tenantId, totalOrdersInDB);

        Long allOrdersCount = clientOrderRepository.countAllOrders(tenantId, from, to);
        Long identifiedOrdersCount = clientOrderRepository.countIdentifiedOrders(tenantId, from, to);
        Long generalOrdersCount = clientOrderRepository.countGeneralOrders(tenantId, from, to);
        
        log.info("En rango de fechas - Total: {}, Identificadas: {}, Generales: {}", allOrdersCount, identifiedOrdersCount, generalOrdersCount);

        // Debug: Obtener muestras de órdenes en el rango
        List<Object[]> ordersInRange = clientOrderRepository.getOrdersInRangeDebug(tenantId, from, to);
        if (ordersInRange != null && !ordersInRange.isEmpty()) {
            log.info("Muestras de órdenes en el rango:");
            for (Object[] order : ordersInRange) {
                log.info("  ID: {}, CustomerID: {}, Fecha: {}, Estado: {}, Total: {}", 
                        order[0], order[1], order[2], order[3], order[4]);
            }
        } else {
            log.warn("No hay órdenes en el rango especificado");
        }

        Object[] identifiedStats = clientOrderRepository.getIdentifiedSalesStats(tenantId, from, to);
        Object[] generalStats = clientOrderRepository.getGeneralSalesStats(tenantId, from, to);

        log.info("Identified Stats: {}", identifiedStats != null ? Arrays.toString(identifiedStats) : "null");
        log.info("General Stats: {}", generalStats != null ? Arrays.toString(generalStats) : "null");

        // Manejar nulls
        if (identifiedStats == null) {
            identifiedStats = new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO};
        }
        if (generalStats == null) {
            generalStats = new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO};
        }

        Long identifiedCount = safeToLong(identifiedStats[0], 0L);
        BigDecimal identifiedRevenue = identifiedStats.length > 1 ? safeToBigDecimal(identifiedStats[1], BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal identifiedAvgTicket = identifiedStats.length > 2 ? safeToBigDecimal(identifiedStats[2], BigDecimal.ZERO) : BigDecimal.ZERO;

        Long generalCount = safeToLong(generalStats[0], 0L);
        BigDecimal generalRevenue = generalStats.length > 1 ? safeToBigDecimal(generalStats[1], BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal generalAvgTicket = generalStats.length > 2 ? safeToBigDecimal(generalStats[2], BigDecimal.ZERO) : BigDecimal.ZERO;

        log.info("Identificadas: count={}, revenue={}, avgTicket={}", identifiedCount, identifiedRevenue, identifiedAvgTicket);
        log.info("Generales: count={}, revenue={}, avgTicket={}", generalCount, generalRevenue, generalAvgTicket);

        BigDecimal totalRevenue = identifiedRevenue.add(generalRevenue);
        BigDecimal identifiedPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? identifiedRevenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal generalPct = BigDecimal.valueOf(100).subtract(identifiedPct);

        return IdentifiedVsGeneralSalesDTO.builder()
                .identifiedOrdersCount(identifiedCount)
                .identifiedRevenue(identifiedRevenue)
                .identifiedAvgTicket(identifiedAvgTicket)
                .generalOrdersCount(generalCount)
                .generalRevenue(generalRevenue)
                .generalAvgTicket(generalAvgTicket)
                .identifiedPercentage(identifiedPct)
                .generalPercentage(generalPct)
                .build();
    }

    @Override
    public List<CustomerLTVDTO> getCustomerLTV(Long tenantId, LocalDateTime from, LocalDateTime to, Integer limit) {
        log.debug("Calculando LTV de clientes para tenant {} entre {} y {}", tenantId, from, to);

        List<Object[]> results = clientOrderRepository.getCustomerLTV(tenantId, from, to);

        return results.stream()
                .limit(limit != null ? limit : Integer.MAX_VALUE)
                .map(row -> CustomerLTVDTO.builder()
                        .customerId(safeToLong(row[0], null))
                        .customerName((String) row[1])
                        .customerEmail((String) row[2])
                        .lifetimeValue(safeToBigDecimal(row[3], BigDecimal.ZERO))
                        .totalOrders(safeToLong(row[4], 0L))
                        .averageOrderValue(safeToBigDecimal(row[5], BigDecimal.ZERO))
                        .firstPurchase(row[6] != null ? (LocalDateTime) row[6] : null)
                        .lastPurchase(row[7] != null ? (LocalDateTime) row[7] : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponConversionRateDTO> getCouponConversionRate(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Calculando tasa de conversión de cupones para tenant {} entre {} y {}", tenantId, from, to);

        List<Object[]> results = clientOrderRepository.getCouponConversionRate(tenantId, from, to);

        return results.stream()
                .map(row -> {
                    Long campaignId = safeToLong(row[0], null);
                    String campaignName = (String) row[1];
                    Long totalIssued = safeToLong(row[2], 0L);
                    Long totalRedeemed = safeToLong(row[3], 0L);
                    Long ordersWithCoupon = safeToLong(row[4], 0L);
                    BigDecimal revenue = safeToBigDecimal(row[5], BigDecimal.ZERO);

                    BigDecimal conversionRate = totalIssued > 0
                            ? BigDecimal.valueOf(ordersWithCoupon * 100.0 / totalIssued).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return CouponConversionRateDTO.builder()
                            .campaignId(campaignId)
                            .campaignName(campaignName)
                            .totalCouponsIssued(totalIssued)
                            .totalCouponsRedeemed(totalRedeemed)
                            .ordersWithCoupon(ordersWithCoupon)
                            .conversionRate(conversionRate)
                            .revenueFromCoupons(revenue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomizationAnalysisDTO> getCustomizationAnalysis(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Analizando personalización de pedidos para tenant {} entre {} y {}", tenantId, from, to);

        List<String> comments = clientOrderItemRepository.findAllCustomizationComments(tenantId, from, to);

        if (comments.isEmpty()) {
            return List.of();
        }

        // Análisis de frecuencia de frases compuestas y palabras clave
        Map<String, Long> phraseFrequency = new HashMap<>();
        long totalComments = comments.size();

        // Frases compuestas comunes en personalización (2-3 palabras)
        List<String> compoundPhrases = Arrays.asList(
                "sin cebolla", "sin tomate", "sin lechuga", "sin salsa", "sin mayo", "sin mayonesa",
                "sin picante", "sin azúcar", "sin sal",
                "con extra", "con doble", "con más",
                "no picante", "poco picante", "muy picante",
                "leche deslactosada", "leche de almendra", "leche de avena", "leche de coco",
                "extra queso", "extra salsa", "extra hielo",
                "bien cocido", "poco cocido", "crudo", "al dente",
                "aparte la salsa", "aparte el dressing", "aparte la mayo"
        );

        for (String comment : comments) {
            String lowerComment = comment.toLowerCase().trim();
            
            // Primero buscar frases compuestas (mayor prioridad)
            boolean foundPhrase = false;
            for (String phrase : compoundPhrases) {
                if (lowerComment.contains(phrase)) {
                    phraseFrequency.merge(phrase, 1L, Long::sum);
                    foundPhrase = true;
                    break; // Una frase por comentario
                }
            }
            
            // Si no encuentra una frase compuesta, extraer el comentario completo como frase personalizada
            if (!foundPhrase && !lowerComment.isEmpty()) {
                // Limpiar y normalizar comentarios completos
                String cleanedComment = lowerComment.replaceAll("\\s+", " ").trim();
                if (cleanedComment.length() >= 3) { // Mínimo 3 caracteres
                    phraseFrequency.merge(cleanedComment, 1L, Long::sum);
                }
            }
        }

        return phraseFrequency.entrySet().stream()
                .map(entry -> CustomizationAnalysisDTO.builder()
                        .keyword(entry.getKey())
                        .frequency(entry.getValue())
                        .percentage(entry.getValue() * 100.0 / totalComments)
                        .build())
                .sorted(Comparator.comparing(CustomizationAnalysisDTO::getFrequency).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignROIDTO> getCampaignROI(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Calculando ROI de campañas para tenant {} entre {} y {}", tenantId, from, to);

        List<Object[]> results = redemptionRepository.findCampaignRoiByDiscountCost(tenantId, from, to);

        return results.stream()
                .map(row -> {
                    Long campaignId = safeToLong(row[0], null);
                    String campaignName = (String) row[1];
                    Long redemptions = safeToLong(row[2], 0L);
                    BigDecimal revenue = safeToBigDecimal(row[3], BigDecimal.ZERO);
                    BigDecimal discountCost = safeToBigDecimal(row[4], BigDecimal.ZERO);

                    BigDecimal profit = revenue.subtract(discountCost);
                    BigDecimal roi = discountCost.compareTo(BigDecimal.ZERO) > 0
                            ? profit.divide(discountCost, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return CampaignROIDTO.builder()
                            .campaignId(campaignId)
                            .campaignName(campaignName)
                            .campaignCost(discountCost)
                            .revenueGenerated(revenue)
                            .profit(profit)
                            .roi(roi)
                            .ordersCount(redemptions)
                            .build();
                })
                .collect(Collectors.toList());
    }
}