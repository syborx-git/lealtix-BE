package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;

/**
 * Punto de una serie temporal para graficas. valorAnterior alimenta la linea
 * comparativa del mismo grafico, en la misma posicion del eje.
 *
 * ordenes y bruto se incluyen porque la hoja de tendencia del .xlsx necesita las
 * tres magnitudes: sin ellas habia que rellenar con cero o repetir el neto.
 */
public record SeriePuntoDTO(
        String etiqueta,
        String periodoIso,
        BigDecimal valor,
        BigDecimal valorAnterior,
        BigDecimal ordenes,
        BigDecimal bruto
) {

    public static SeriePuntoDTO de(String etiqueta, String periodoIso, BigDecimal valor, BigDecimal valorAnterior) {
        return new SeriePuntoDTO(
                etiqueta,
                periodoIso,
                valor == null ? BigDecimal.ZERO : valor,
                valorAnterior == null ? BigDecimal.ZERO : valorAnterior,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    public static SeriePuntoDTO completo(String etiqueta, String periodoIso, BigDecimal ordenes,
                                          BigDecimal bruto, BigDecimal neto, BigDecimal netoAnterior) {
        return new SeriePuntoDTO(
                etiqueta,
                periodoIso,
                neto == null ? BigDecimal.ZERO : neto,
                netoAnterior == null ? BigDecimal.ZERO : netoAnterior,
                ordenes == null ? BigDecimal.ZERO : ordenes,
                bruto == null ? BigDecimal.ZERO : bruto
        );
    }
}
