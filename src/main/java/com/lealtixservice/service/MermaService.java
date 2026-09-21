package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.MermaRequest;

import java.util.UUID;

public interface MermaService {

    GenericResponse registrarMerma(MermaRequest request);

    GenericResponse registrarMermaAdministrativa(MermaRequest request);

    GenericResponse listarPorTenant(Long tenantId);

    GenericResponse resolverInsumosUsados(UUID orderId);
}