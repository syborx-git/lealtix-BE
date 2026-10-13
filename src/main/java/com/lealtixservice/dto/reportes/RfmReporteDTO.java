package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 3.1 - Análisis RFM de Clientes.
 *
 * Segmentación basada en Recencia (última visita), Frecuencia (visitas) y Valor Monetario (gasto total).
 */
public record RfmReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO resumenSegmentos,
        TablaReporteDTO clientesDetalle,
        List<ChartPuntoDTO> distribucionSegmentos,
        List<ChartPuntoDTO> ingresosPorSegmento
) {
}
