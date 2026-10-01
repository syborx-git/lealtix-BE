package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Respuesta del reporte 1.1 (Dashboard de Ventas y Tendencias).
 * Sigue el envelope comun: meta + kpis comparativos + serie + tablas.
 */
public record VentasTendenciasDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        List<SeriePuntoDTO> serie,
        TablaReporteDTO porCategoria,
        TablaReporteDTO topProductos
) {
}
