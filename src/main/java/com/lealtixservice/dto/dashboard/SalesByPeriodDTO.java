package com.lealtixservice.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ventas agrupadas por periodo (día/semana/mes), con desglose de ventas
 * identificadas (clientes registrados) vs generales (anónimas).
 */
public record SalesByPeriodDTO(
        LocalDate periodStart,
        BigDecimal totalSales,
        BigDecimal identifiedSales,
        BigDecimal generalSales
) {}
