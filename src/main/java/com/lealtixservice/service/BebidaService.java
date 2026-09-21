package com.lealtixservice.service;

import com.lealtixservice.dto.CrearBebidaRequest;
import com.lealtixservice.dto.GenericResponse;

public interface BebidaService {

    /** Catálogo para POS: stock disponible calculado según tipo_bebida. */
    GenericResponse catalogoPos(Long tenantId);

    /** Insert transaccional: bebida + receta (solo preparadas) en la misma transacción. */
    GenericResponse crearBebida(CrearBebidaRequest request);
}