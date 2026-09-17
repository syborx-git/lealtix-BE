package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.StockRequestRequest;

public interface StockRequestService {

    GenericResponse crearSolicitud(StockRequestRequest request);

    GenericResponse listarPorTenant(Long tenantId);

    GenericResponse contarPendientesPorArea(Long tenantId);
}