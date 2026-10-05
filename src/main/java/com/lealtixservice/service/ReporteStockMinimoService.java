package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.StockMinimoReporteDTO;

/**
 * Servicio para el Reporte 2.4: Alertas de Stock Minimo y Critico.
 */
public interface ReporteStockMinimoService {

    StockMinimoReporteDTO obtener(Long tenantId);

    byte[] exportar(Long tenantId);
}
