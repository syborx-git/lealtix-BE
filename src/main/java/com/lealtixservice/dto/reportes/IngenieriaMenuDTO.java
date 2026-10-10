package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Respuesta del reporte 2.1 (Ingeniería de Menú - Matriz Kasavana & Smith).
 * Clasifica platillos en Estrellas, Caballos de batalla, Rompecabezas y Perros
 * según su popularidad (volumen de ventas) y rentabilidad (margen de contribución).
 */
public record IngenieriaMenuDTO(
        ReporteMetaDTO meta,
        List<KpiDTO> kpis,
        Double umbralPopularidad,
        BigDecimal umbralMargen,
        Map<String, CuadranteResumenDTO> resumenCuadrantes,
        TablaReporteDTO matrizProductos,
        List<PuntoMatrizDTO> puntosGrafica
) {
    public static IngenieriaMenuDTO de(
            ReporteMetaDTO meta,
            List<KpiDTO> kpis,
            Double umbralPopularidad,
            BigDecimal umbralMargen,
            Map<String, CuadranteResumenDTO> resumenCuadrantes,
            TablaReporteDTO matrizProductos,
            List<PuntoMatrizDTO> puntosGrafica
    ) {
        return new IngenieriaMenuDTO(
                meta,
                kpis == null ? List.of() : kpis,
                umbralPopularidad,
                umbralMargen,
                resumenCuadrantes == null ? Map.of() : resumenCuadrantes,
                matrizProductos,
                puntosGrafica == null ? List.of() : puntosGrafica
        );
    }
}
