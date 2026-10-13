package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.CampanasReporteDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface ReporteCampanasService {
    CampanasReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
