package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Reporte 2.2 - Kardex de Insumos.
 *
 * Registra los movimientos de inventario: entradas/restock, consumo en ventas y mermas,
 * con saldo final y desglose por insumo.
 */
public record KardexReporteDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO resumenInsumos,
        TablaReporteDTO movimientosDetalle,
        TablaReporteDTO transferenciasAreas,
        List<ChartPuntoDTO> distribucionMovimientos,
        List<ChartPuntoDTO> topInsumosRotacion
) {
}
