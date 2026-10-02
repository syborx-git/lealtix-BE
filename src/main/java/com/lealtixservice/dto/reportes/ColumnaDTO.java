package com.lealtixservice.dto.reportes;

/**
 * Columna de una tabla de reporte. El key es el contrato entre el JSON,
 * la tabla del frontend y las celdas del Excel.
 */
public record ColumnaDTO(
        String key,
        String label,
        TipoColumna tipo,
        Integer ancho
) {

    public static ColumnaDTO de(String key, String label, TipoColumna tipo) {
        return new ColumnaDTO(key, label, tipo, null);
    }
}
