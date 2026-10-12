package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.MapaCalorReporteDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface ReporteMapaCalorService {
    MapaCalorReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
