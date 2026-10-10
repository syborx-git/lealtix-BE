package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.CorteInsumosDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface ReporteCorteInsumosService {
    CorteInsumosDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area);
}
