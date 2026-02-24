package com.lealtixservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * DTO para resumen de ventas totales.
 * 
 * Contiene totales, promedios y contadores de TODAS las transacciones sin filtros:
 * - Órdenes con cliente identificado o sin cliente (ventas generales)
 * - Órdenes con cupón redimido o sin cupón
 * 
 * Campos:
 * - totalSales: Suma total del campo 'total' de todas las órdenes
 * - avgTicket: Promedio del campo 'total' de todas las órdenes
 * - transactionCount: Cantidad total de órdenes
 */
public record SalesSummaryDTO(
        BigDecimal totalSales,
        BigDecimal avgTicket,
        Long transactionCount
) {}

