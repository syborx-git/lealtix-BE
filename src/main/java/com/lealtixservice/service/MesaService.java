package com.lealtixservice.service;

import com.lealtixservice.dto.AsignarMeseroRequest;
import com.lealtixservice.dto.MesaDTO;
import com.lealtixservice.dto.MesaRequest;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.MesaForma;

import java.util.List;

public interface MesaService {
    List<MesaDTO> listByTenant(Long tenantId);

    List<MesaDTO> listByTenantAndEstado(Long tenantId, MesaEstado estado);

    MesaDTO getById(Long id, Long tenantId);

    MesaDTO create(Long tenantId, MesaRequest request);

    MesaDTO update(Long id, Long tenantId, MesaRequest request);

    MesaDTO assignMesero(Long id, Long tenantId, AsignarMeseroRequest request);

    MesaDTO changeEstado(Long id, Long tenantId, MesaEstado estado);

    MesaDTO updatePosicion(Long id, Long tenantId, Double posicionX, Double posicionY);

    MesaDTO updateForma(Long id, Long tenantId, MesaForma forma);

    List<MesaDTO> unirMesas(Long tenantId, List<Long> mesaIds);

    List<MesaDTO> separarGrupo(Long tenantId, String grupoId);

    void delete(Long id, Long tenantId);
}