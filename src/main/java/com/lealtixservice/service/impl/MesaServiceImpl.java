package com.lealtixservice.service.impl;

import com.lealtixservice.dto.AsignarMeseroRequest;
import com.lealtixservice.dto.MesaDTO;
import com.lealtixservice.dto.MesaRequest;
import com.lealtixservice.entity.Mesa;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.MesaForma;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.repository.MesaRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.MesaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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
                .posicionX(request.getPosicionX())
                .posicionY(request.getPosicionY())
                .forma(request.getForma() != null ? request.getForma() : MesaForma.cuadrada)
                .rotacion(normalizeRotacion(request.getRotacion()))
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
        if (request.getForma() != null) {
            mesa.setForma(request.getForma());
        }
        if (request.getRotacion() != null) {
            mesa.setRotacion(normalizeRotacion(request.getRotacion()));
        }
        if (request.getPosicionX() != null) {
            mesa.setPosicionX(request.getPosicionX());
        }
        if (request.getPosicionY() != null) {
            mesa.setPosicionY(request.getPosicionY());
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
    public MesaDTO updatePosicion(Long id, Long tenantId, Double posicionX, Double posicionY) {
        Mesa mesa = getMesa(id, tenantId);
        if (posicionX == null || posicionY == null) {
            throw new IllegalArgumentException("La posición es requerida (posicionX y posicionY)");
        }
        mesa.setPosicionX(posicionX);
        mesa.setPosicionY(posicionY);
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public MesaDTO updateForma(Long id, Long tenantId, MesaForma forma) {
        Mesa mesa = getMesa(id, tenantId);
        if (forma == null) {
            throw new IllegalArgumentException("La forma es requerida");
        }
        mesa.setForma(forma);
        mesa = mesaRepository.save(mesa);
        return MesaDTO.fromEntity(mesa, resolveMeseroName(mesa));
    }

    @Override
    @Transactional
    public List<MesaDTO> unirMesas(Long tenantId, List<Long> mesaIds) {
        if (mesaIds == null || mesaIds.size() < 2) {
            throw new IllegalArgumentException("Selecciona al menos dos mesas para unir");
        }

        List<Mesa> mesas = mesaIds.stream().distinct().map(id -> {
            Mesa m = getMesa(id, tenantId);
            if (m.getEstado() != MesaEstado.LIBRE) {
                throw new IllegalArgumentException("Solo puedes unir mesas libres. '" + m.getNombre() + "' está " + m.getEstado());
            }
            return m;
        }).collect(Collectors.toList());

        String grupoId = UUID.randomUUID().toString();
        mesas.forEach(m -> {
            m.setIdGrupoTemporal(grupoId);
            m.setEstado(MesaEstado.LIBRE);
        });
        mesaRepository.saveAll(mesas);

        return mesas.stream()
                .map(m -> MesaDTO.fromEntity(m, resolveMeseroName(m)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MesaDTO> separarGrupo(Long tenantId, String grupoId) {
        if (grupoId == null || grupoId.isBlank()) {
            throw new IllegalArgumentException("El grupo es requerido");
        }
        List<Mesa> mesas = mesaRepository.findByTenantIdAndIdGrupoTemporal(tenantId, grupoId);
        mesas.forEach(m -> m.setIdGrupoTemporal(null));
        mesaRepository.saveAll(mesas);

        return mesas.stream()
                .map(m -> MesaDTO.fromEntity(m, resolveMeseroName(m)))
                .collect(Collectors.toList());
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

    private Integer normalizeRotacion(Integer rotacion) {
        if (rotacion == null) {
            return 0;
        }
        return ((rotacion % 360) + 360) % 360;
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