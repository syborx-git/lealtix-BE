package com.lealtixservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * Ventas agrupadas por categoría de producto.
 */
public record SalesByCategoryDTO(
        String categoryName,
        BigDecimal totalSales
) {}
