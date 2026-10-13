package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 3.2 - Rendimiento de Campañas y Cupones.
 *
 * Mide la efectividad de promociones: cupones canjeados, descuentos otorgados,
 * ventas generadas y tasa de retorno.
 */
public record CampanasReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO resumenCampanas,
        TablaReporteDTO cuponesCanjeados,
        List<ChartPuntoDTO> canjesPorCampana,
        List<ChartPuntoDTO> comparativaDescuentoVenta
) {
}
