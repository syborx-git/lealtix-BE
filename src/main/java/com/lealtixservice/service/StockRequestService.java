package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.StockRequestRequest;

public interface StockRequestService {

    GenericResponse crearSolicitud(StockRequestRequest request);

    GenericResponse listarPorTenant(Long tenantId);

    GenericResponse contarPendientesPorArea(Long tenantId);

    /** Solicitudes PENDIENTE, opcionalmente filtradas por área (COCINA|BARRA). */
    GenericResponse listarPendientesDetalle(Long tenantId, String area);

    /** Acepta la solicitud: descuenta de bodega y distribuye al área solicitante. */
    GenericResponse aceptarSolicitud(Long id, Long tenantId);
}