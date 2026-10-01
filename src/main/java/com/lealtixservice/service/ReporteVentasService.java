package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.VentasTendenciasDTO;

import java.time.LocalDateTime;

/**
 * Reporte 1.1: Dashboard de Ventas y Tendencias (Pilar 1 - Finanzas y Ventas).
 *
 * Cada metodo resuelve internamente el rango actual y su periodo anterior
 * equivalente, de modo que el llamador nunca construye comparativas a mano.
 */
public interface ReporteVentasService {

    /**
     * Reporte en JSON para la vista del dashboard.
     *
     * @param tenantId      tenant (validado contra el JWT por el controller)
     * @param preset        filtro rapido; null equivale a HOY
     * @param from          fecha inicial, solo para PERSONALIZADO
     * @param to            fecha final, solo para PERSONALIZADO
     * @param granularidad  day, week o month; si es null se elige por longitud del rango
     */
    VentasTendenciasDTO obtener(Long tenantId, PresetReporte preset,
                                LocalDateTime from, LocalDateTime to, String granularidad);

    /**
     * Mismo reporte serializado a .xlsx con las mismas hojas que la vista web.
     */
    byte[] exportar(Long tenantId, PresetReporte preset,
                    LocalDateTime from, LocalDateTime to, String granularidad);
}
