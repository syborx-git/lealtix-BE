package com.lealtixservice.dto.reportes;

/**
 * Salida del motor de tiempo: el periodo actual a reportar y su periodo
 * anterior equivalente, que es contra el que se calcula la variacion.
 */
public record PeriodoComparativo(
        PresetReporte preset,
        RangoDTO actual,
        RangoDTO anterior
) {
}
