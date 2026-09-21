package com.lealtixservice.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Costos operacionales calculados automáticamente con los datos reales
 * de los últimos 2 meses: restock de insumos (materia prima), sueldos
 * (recurso humano) y ventas totales.
 */
public record OperationalCostsDTO(
        LocalDateTime desde,
        LocalDateTime hasta,
        BigDecimal ventasTotales,
        BigDecimal costoMateriaPrima,
        BigDecimal costoSueldos,
        BigDecimal costoTotal,
        BigDecimal ganancias,
        double porcentajeMateriaPrima,
        double porcentajeRecursoHumano,
        double porcentajeCostoTotal,
        double porcentajeGanancias,
        long cantidadRestocks
) {
}
