package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;

public record ChartPuntoDTO(
        String etiqueta,
        Double valor,
        BigDecimal valorMoneda,
        String unidad,
        String categoria,
        String area
) {
    public static ChartPuntoDTO de(String etiqueta, Double valor, String unidad, String categoria, String area) {
        return new ChartPuntoDTO(etiqueta, valor, BigDecimal.ZERO, unidad, categoria, area);
    }

    public static ChartPuntoDTO deMoneda(String etiqueta, Double valor, BigDecimal valorMoneda, String unidad, String categoria, String area) {
        return new ChartPuntoDTO(etiqueta, valor, valorMoneda, unidad, categoria, area);
    }
}
