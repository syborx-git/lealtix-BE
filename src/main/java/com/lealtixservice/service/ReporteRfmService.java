package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.RfmReporteDTO;

import java.time.LocalDateTime;

public interface ReporteRfmService {
    RfmReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
