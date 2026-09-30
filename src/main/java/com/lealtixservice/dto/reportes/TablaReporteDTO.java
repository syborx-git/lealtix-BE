package com.lealtixservice.dto.reportes;

import java.util.List;
import java.util.Map;

/**
 * Tabla de reporte autodescriptiva: la misma estructura alimenta la tabla del
 * frontend y las hojas del Excel, de modo que anadir un reporte no obliga a
 * tocar el generador de archivos.
 */
public record TablaReporteDTO(
        List<ColumnaDTO> columnas,
        List<Map<String, Object>> filas
) {

    public static TablaReporteDTO de(List<ColumnaDTO> columnas, List<Map<String, Object>> filas) {
        return new TablaReporteDTO(columnas, filas == null ? List.of() : filas);
    }
}
