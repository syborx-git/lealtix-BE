package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 4.1 - Auditoría de Tickets Cancelados.
 *
 * Consolida las comandas canceladas con justificación/motivo, usuario responsable,
 * mesa, fecha y hora, monto económico no recaudado y comparativa contra el período anterior.
 */
public record AuditoriaTicketsCanceladosDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO porMotivo,
        TablaReporteDTO porResponsable,
        TablaReporteDTO detalle
) {
    public static AuditoriaTicketsCanceladosDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            TablaReporteDTO porMotivo,
            TablaReporteDTO porResponsable,
            TablaReporteDTO detalle
    ) {
        return new AuditoriaTicketsCanceladosDTO(meta, kpis, porMotivo, porResponsable, detalle);
    }
}
