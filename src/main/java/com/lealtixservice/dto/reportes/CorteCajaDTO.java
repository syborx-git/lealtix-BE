package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 1.2 - Corte de Caja y Conciliacion.
 *
 * Los KPIs, la comparativa contra el periodo anterior equivalente y el desglose
 * por metodo de pago se calculan aqui en el servidor. La UI solo los pinta.
 */
public record CorteCajaDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO porMetodo,
        TablaReporteDTO porCajero,
        TablaReporteDTO anulaciones
) {

    public static CorteCajaDTO de(ReporteMetaDTO meta,
                                  List<KpiDTO> kpis,
                                  TablaReporteDTO porMetodo,
                                  TablaReporteDTO porCajero,
                                  TablaReporteDTO anulaciones) {
        return new CorteCajaDTO(meta, kpis, porMetodo, porCajero, anulaciones);
    }
}
