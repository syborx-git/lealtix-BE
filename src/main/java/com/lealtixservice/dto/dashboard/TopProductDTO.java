package com.lealtixservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * Producto más vendido: nombre, cantidad total vendida e ingresos generados.
 */
public record TopProductDTO(
        String productName,
        Long totalQuantity,
        BigDecimal totalRevenue
) {}
