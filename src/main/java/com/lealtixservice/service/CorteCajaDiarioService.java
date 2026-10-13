package com.lealtixservice.service;

import com.lealtixservice.dto.corte.CorteCajaDiarioDTO;
import com.lealtixservice.dto.corte.EstadoCorteDiarioHoyDTO;
import com.lealtixservice.dto.corte.GenerarCorteDiarioRequest;
import com.lealtixservice.dto.reportes.AuditoriaCortesDiariosDTO;
import com.lealtixservice.dto.reportes.PresetReporte;

import java.time.LocalDateTime;

public interface CorteCajaDiarioService {

    EstadoCorteDiarioHoyDTO consultarEstadoHoy(Long tenantId);

    CorteCajaDiarioDTO generarCorteDiario(GenerarCorteDiarioRequest request, Long userId, String userEmail);

    AuditoriaCortesDiariosDTO obtenerAuditoria(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);

    byte[] exportarExcel(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to);
}
