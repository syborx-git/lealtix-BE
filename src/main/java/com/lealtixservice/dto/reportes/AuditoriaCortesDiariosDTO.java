package com.lealtixservice.dto.reportes;

import com.lealtixservice.dto.corte.CorteCajaDiarioDTO;
import java.util.List;

/**
 * Reporte 4.2 - Auditoría de Cortes del Día.
 *
 * Registra y concilia los movimientos de corte y cierre de caja diario,
 * comparando montos teóricos calculados por el sistema vs dinero físico contado,
 * identificando sobrantes, faltantes, causas documentadas y trazabilidad por cajero.
 */
public record AuditoriaCortesDiariosDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        TablaReporteDTO porMetodo,
        TablaReporteDTO distribucion,
        TablaReporteDTO detalle,
        List<CorteCajaDiarioDTO> listaCortes
) {
    public static AuditoriaCortesDiariosDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            TablaReporteDTO porMetodo,
            TablaReporteDTO distribucion,
            TablaReporteDTO detalle,
            List<CorteCajaDiarioDTO> listaCortes
    ) {
        return new AuditoriaCortesDiariosDTO(meta, kpis, porMetodo, distribucion, detalle, listaCortes);
    }
}
