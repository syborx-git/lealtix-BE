package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.AuditoriaTicketsCanceladosDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

/**
 * Servicio para el Reporte 4.1: Auditoría de Tickets Cancelados.
 */
public interface ReporteTicketsCanceladosService {

    AuditoriaTicketsCanceladosDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);

    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
