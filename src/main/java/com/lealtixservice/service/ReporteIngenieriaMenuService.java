package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.IngenieriaMenuDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface ReporteIngenieriaMenuService {
    IngenieriaMenuDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area);
    byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area);
}
