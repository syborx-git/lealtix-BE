package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 4.5 - Mapa de Calor de Afluencia.
 *
 * Mapeo de volumen de comandas y ventas por hora del día y día de la semana para detectar horas pico.
 */
public record MapaCalorReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO matrizAfluencia,
        TablaReporteDTO desglosePorHora,
        List<ChartPuntoDTO> afluenciaPorHora,
        List<ChartPuntoDTO> afluenciaPorDia
) {
}
