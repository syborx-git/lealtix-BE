package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.CorteCajaDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface ReporteCorteCajaService {

    CorteCajaDTO obtener(Long tenantId, PresetReporte preset,
                         LocalDateTime from, LocalDateTime to);

    byte[] exportar(Long tenantId, PresetReporte preset,
                    LocalDateTime from, LocalDateTime to);
}
