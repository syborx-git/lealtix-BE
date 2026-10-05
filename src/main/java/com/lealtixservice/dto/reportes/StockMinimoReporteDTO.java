package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 2.4 - Alertas de Stock Mínimo y Crítico.
 *
 * Consolida los insumos (bodega, cocina, barra), bebidas y productos de menú cuyo stock
 * está por debajo o igual al stock mínimo configurado (o agotados en 0).
 * Calcula la sugerencia de compra/reabastecimiento y el costo estimado de reposición.
 */
public record StockMinimoReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO alertas,
        TablaReporteDTO listaCompras,
        TablaReporteDTO inventarioGeneral
) {
    public static StockMinimoReporteDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            TablaReporteDTO alertas,
            TablaReporteDTO listaCompras,
            TablaReporteDTO inventarioGeneral
    ) {
        return new StockMinimoReporteDTO(meta, kpis, alertas, listaCompras, inventarioGeneral);
    }
}
