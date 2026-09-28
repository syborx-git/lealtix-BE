package com.lealtixservice.dto.reportes;

import java.time.LocalDateTime;

/**
 * Metadatos del reporte: identificacion, pilar y los dos periodos comparados.
 * Viaja en todas las respuestas para que la UI nunca tenga que adivinar de
 * donde salio el % de crecimiento que esta mostrando.
 */
public record ReporteMetaDTO(
        String reporteKey,
        String pilar,
        String nombre,
        PresetReporte preset,
        RangoDTO rangoActual,
        RangoDTO rangoAnterior,
        String granularidad,
        LocalDateTime generadoEn
) {

    public static ReporteMetaDTO de(String reporteKey, String pilar, String nombre,
                                    PeriodoComparativo periodo, String granularidad) {
        return new ReporteMetaDTO(
                reporteKey,
                pilar,
                nombre,
                periodo.preset(),
                periodo.actual(),
                periodo.anterior(),
                granularidad,
                LocalDateTime.now()
        );
    }
}
