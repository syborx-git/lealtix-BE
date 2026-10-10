package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Respuesta del reporte 2.5 (Corte Diario de Insumos - Cocina / Barra).
 * Refleja platillos vendidos, consumo de insumos derivados y stock remanente en inventario.
 */
public record CorteInsumosDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        String areaFiltro,
        TablaReporteDTO platillosVendidos,
        TablaReporteDTO insumosConsumidos,
        TablaReporteDTO stockDisponible,
        List<ChartPuntoDTO> topPlatillos,
        List<ChartPuntoDTO> topInsumos
) {
    public static CorteInsumosDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            String areaFiltro,
            TablaReporteDTO platillosVendidos,
            TablaReporteDTO insumosConsumidos,
            TablaReporteDTO stockDisponible,
            List<ChartPuntoDTO> topPlatillos,
            List<ChartPuntoDTO> topInsumos
    ) {
        return new CorteInsumosDTO(
                meta,
                kpis == null ? List.of() : kpis,
                areaFiltro,
                platillosVendidos,
                insumosConsumidos,
                stockDisponible,
                topPlatillos == null ? List.of() : topPlatillos,
                topInsumos == null ? List.of() : topInsumos
        );
    }
}
