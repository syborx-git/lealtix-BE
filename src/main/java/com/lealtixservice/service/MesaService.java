package com.lealtixservice.service;

import com.lealtixservice.dto.AsignarMeseroRequest;
import com.lealtixservice.dto.MesaDTO;
import com.lealtixservice.dto.MesaRequest;
import com.lealtixservice.enums.MesaEstado;

import java.util.List;

public interface MesaService {
    List<MesaDTO> listByTenant(Long tenantId);

    List<MesaDTO> listByTenantAndEstado(Long tenantId, MesaEstado estado);

    MesaDTO getById(Long id, Long tenantId);

    MesaDTO create(Long tenantId, MesaRequest request);

    MesaDTO update(Long id, Long tenantId, MesaRequest request);

    MesaDTO assignMesero(Long id, Long tenantId, AsignarMeseroRequest request);

    MesaDTO changeEstado(Long id, Long tenantId, MesaEstado estado);

    void delete(Long id, Long tenantId);
}