package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 4.3 - Tiempos de Servicio y Preparación.
 *
 * Mide el rendimiento operativo y tiempos de respuesta de cocina y barra por comanda.
 */
public record TiemposServicioReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO tiemposPorEstacion,
        TablaReporteDTO detalleComandas,
        List<ChartPuntoDTO> distribucionRangos,
        List<ChartPuntoDTO> promedioPorEstacion
) {
}
