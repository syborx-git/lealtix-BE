package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 4.4 - Rendimiento por Mesero.
 *
 * Analiza ventas, tickets atendidos, ticket promedio, platillos vendidos y propinas por mesero.
 */
public record RendimientoMeseroReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO tablaMeseros,
        TablaReporteDTO desgloseMetodosPago,
        List<ChartPuntoDTO> ventasPorMesero,
        List<ChartPuntoDTO> ticketsPorMesero
) {
}
