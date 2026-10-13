package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;

public record PuntoMatrizDTO(
        Long productoId,
        String nombre,
        String categoria,
        String cuadrante,
        Double volumenX,
        BigDecimal margenY,
        BigDecimal precio,
        BigDecimal costo,
        Double margenPct,
        Long unidadesVendidas,
        BigDecimal ingresosTotales,
        BigDecimal margenTotal
) {
}
