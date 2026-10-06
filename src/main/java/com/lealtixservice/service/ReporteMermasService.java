package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.AuditoriaMermasDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

/**
 * Servicio para el Reporte 2.3: Auditoria de Mermas.
 */
public interface ReporteMermasService {

    AuditoriaMermasDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);

    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
