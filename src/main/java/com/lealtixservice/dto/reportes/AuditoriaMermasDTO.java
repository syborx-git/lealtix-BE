package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 2.3 - Auditoria de Mermas.
 *
 * Consolida las salidas no-venta (operativas/comanda y administrativas de bodega, cocina o barra),
 * con costeo total de perdidas, desglose por motivo, responsable, insumos mermados y comparativa
 * contra el periodo anterior equivalente.
 */
public record AuditoriaMermasDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO porMotivo,
        TablaReporteDTO porResponsable,
        TablaReporteDTO porInsumo,
        TablaReporteDTO detalle
) {
    public static AuditoriaMermasDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            TablaReporteDTO porMotivo,
            TablaReporteDTO porResponsable,
            TablaReporteDTO porInsumo,
            TablaReporteDTO detalle
    ) {
        return new AuditoriaMermasDTO(meta, kpis, porMotivo, porResponsable, porInsumo, detalle);
    }
}
