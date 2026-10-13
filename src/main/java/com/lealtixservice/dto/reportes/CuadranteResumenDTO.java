package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;

public record CuadranteResumenDTO(
        String cuadrante,
        String etiqueta,
        String icono,
        String color,
        int cantidadPlatillos,
        long unidadesVendidas,
        BigDecimal ingresosTotales,
        BigDecimal margenTotal,
        Double porcentajeVentas,
        Double porcentajeMargen,
        String estrategia
) {
}
