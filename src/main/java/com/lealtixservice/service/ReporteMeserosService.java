package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.RendimientoMeseroReporteDTO;

import java.time.LocalDateTime;

public interface ReporteMeserosService {
    RendimientoMeseroReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
