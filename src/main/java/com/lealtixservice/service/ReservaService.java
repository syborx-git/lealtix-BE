package com.lealtixservice.service;

import com.lealtixservice.dto.ReservaDTO;
import com.lealtixservice.dto.ReservaRequest;

import java.util.List;

public interface ReservaService {
    List<ReservaDTO> listByTenant(Long tenantId);

    ReservaDTO getById(Long id, Long tenantId);

    ReservaDTO create(Long tenantId, ReservaRequest request);

    ReservaDTO update(Long id, Long tenantId, ReservaRequest request);

    ReservaDTO cancel(Long id, Long tenantId);

    void delete(Long id, Long tenantId);
}