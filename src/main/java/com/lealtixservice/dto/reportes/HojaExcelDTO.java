package com.lealtixservice.dto.reportes;

import java.util.List;

/**
 * Hoja generica del Excel. Todo reporte se exporta como un conjunto de hojas,
 * por lo que el servicio de Excel no necesita conocer ningun reporte concreto.
 *
 * "tipos" aplica el mismo formato a todas las filas. Cuando un reporte mezcla
 * formatos en la misma columna (por ejemplo un resumen de KPIs donde "Total
 * cobrado" es moneda y "Operaciones" es un conteo), se usa tiposPorFila para
 * dar el formato correcto a cada fila sin partir la hoja en dos.
 */
public record HojaExcelDTO(
        String nombre,
        String titulo,
        String subtitulo,
        List<String> encabezados,
        List<TipoColumna> tipos,
        List<List<Object>> filas,
        List<List<TipoColumna>> tiposPorFila
) {

    public static HojaExcelDTO de(String nombre, String titulo, String subtitulo,
                                  List<String> encabezados, List<TipoColumna> tipos,
                                  List<List<Object>> filas) {
        return new HojaExcelDTO(
                nombre,
                titulo,
                subtitulo,
                encabezados == null ? List.of() : encabezados,
                tipos == null ? List.of() : tipos,
                filas == null ? List.of() : filas,
                List.of()
        );
    }

    /** Igual que de(...) pero con un tipo por celda, permitiendo formato mixto por fila. */
    public static HojaExcelDTO deTiposPorFila(String nombre, String titulo, String subtitulo,
                                              List<String> encabezados,
                                              List<List<TipoColumna>> tiposPorFila,
                                              List<List<Object>> filas) {
        return new HojaExcelDTO(
                nombre,
                titulo,
                subtitulo,
                encabezados == null ? List.of() : encabezados,
                List.of(),
                filas == null ? List.of() : filas,
                tiposPorFila == null ? List.of() : tiposPorFila
        );
    }
}
