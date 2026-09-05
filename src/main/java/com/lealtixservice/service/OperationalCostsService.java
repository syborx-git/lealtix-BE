package com.lealtixservice.service;

import com.lealtixservice.dto.dashboard.OperationalCostsDTO;

/**
 * Calcula los costos operacionales de forma automática a partir de los
 * gastos reales de los últimos 2 meses (restock de insumos + sueldos)
 * y las ventas totales generadas en ese periodo.
 */
public interface OperationalCostsService {

    /**
     * @param tenantId Identificador del negocio.
     * @param months Número de meses hacia atrás a considerar (por defecto 2).
     * @return Costos operacionales calculados con datos reales.
     */
    OperationalCostsDTO calculate(Long tenantId, int months);
}
