package com.lealtixservice.service.impl;

import com.lealtixservice.dto.AsignarMeseroRequest;
import com.lealtixservice.dto.MesaDTO;
import com.lealtixservice.dto.MesaRequest;
import com.lealtixservice.entity.Mesa;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.repository.MesaRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.MesaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MesaServiceImpl implements MesaService {

    private final MesaRepository mesaRepository;
    private final TenantUserRepository tenantUserRepository;

    public MesaServiceImpl(MesaRepository mesaRepository, TenantUserRepository tenantUserRepository) {
        this.mesaRepository = mesaRepository;
        this.tenantUserRepository = tenantUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MesaDTO> listByTenant(Long tenantId) {
        return mesaRepository.findByTenantIdOrderByNumeroAscNombreAsc(tenantId).stream()
                .map(m -> MesaDTO.fromEntity(m, resolveMeseroName(m)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MesaDTO> listByTenantAndEstado(Long tenantId, MesaEstado estado) {
        return mesaRepository.findByTenantIdAndEstadoOrderByNumeroAsc(tenantId, estado).stream()
                .map(m -> MesaDTO.fromEntity(m, resolveMeseroName(m)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MesaDTO getById(Long id, Long tenantId) {
        Mesa mesa = getMesa(id, tenantId);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public MesaDTO create(Long tenantId, MesaRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la mesa es requerido");
        }
        if (mesaRepository.existsByTenantIdAndNombre(tenantId, request.getNombre().trim())) {
            throw new IllegalArgumentException("Ya existe una mesa con ese nombre en este local");
        }
        validateMeseroIfPresent(tenantId, request.getMeseroUserId());

        Mesa mesa = Mesa.builder()
                .tenantId(tenantId)
                .nombre(request.getNombre().trim())
                .numero(request.getNumero())
                .capacidad(request.getCapacidad() != null ? request.getCapacidad() : 4)
                .estado(request.getEstado() != null ? request.getEstado() : MesaEstado.LIBRE)
                .meseroUserId(request.getMeseroUserId())
                .build();
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public MesaDTO update(Long id, Long tenantId, MesaRequest request) {
        Mesa mesa = getMesa(id, tenantId);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nombre = request.getNombre().trim();
            if (mesaRepository.existsByTenantIdAndNombreAndIdNot(tenantId, nombre, id)) {
                throw new IllegalArgumentException("Ya existe una mesa con ese nombre en este local");
            }
            mesa.setNombre(nombre);
        }
        if (request.getNumero() != null) {
            mesa.setNumero(request.getNumero());
        }
        if (request.getCapacidad() != null) {
            mesa.setCapacidad(request.getCapacidad());
        }
        validateMeseroIfPresent(tenantId, request.getMeseroUserId());
        mesa.setMeseroUserId(request.getMeseroUserId());
        if (request.getEstado() != null) {
            applyEstado(mesa, request.getEstado());
        }
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public MesaDTO assignMesero(Long id, Long tenantId, AsignarMeseroRequest request) {
        Mesa mesa = getMesa(id, tenantId);
        validateMeseroIfPresent(tenantId, request.getMeseroUserId());
        mesa.setMeseroUserId(request.getMeseroUserId());
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public MesaDTO changeEstado(Long id, Long tenantId, MesaEstado estado) {
        Mesa mesa = getMesa(id, tenantId);
        if (estado == null) {
            throw new IllegalArgumentException("El estado es requerido");
        }
        applyEstado(mesa, estado);
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public void delete(Long id, Long tenantId) {
        Mesa mesa = getMesa(id, tenantId);
        mesaRepository.delete(mesa);
    }

    private Mesa getMesa(Long id, Long tenantId) {
        return mesaRepository.findById(id)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada para este local"));
    }

    private void applyEstado(Mesa mesa, MesaEstado estado) {
        mesa.setEstado(estado);
        if (estado == MesaEstado.LIBRE) {
            mesa.setMeseroUserId(null);
        }
    }

    private void validateMeseroIfPresent(Long tenantId, Long meseroUserId) {
        if (meseroUserId == null) {
            return;
        }
        TenantUser user = tenantUserRepository.findByIdAndTenantId(meseroUserId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("El mesero no existe en este local"));
        if (user.getRol() != RoleEnum.MESERO) {
            throw new IllegalArgumentException("El usuario seleccionado no tiene rol de mesero");
        }
    }

    private String resolveMeseroName(Mesa mesa) {
        if (mesa.getMeseroUserId() == null) {
            return null;
        }
        return tenantUserRepository.findById(mesa.getMeseroUserId())
                .map(TenantUser::getNombre)
                .orElse(null);
    }
}