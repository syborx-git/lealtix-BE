package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ReservaDTO;
import com.lealtixservice.dto.ReservaRequest;
import com.lealtixservice.entity.Mesa;
import com.lealtixservice.entity.Reserva;
import com.lealtixservice.enums.ReservaEstado;
import com.lealtixservice.repository.MesaRepository;
import com.lealtixservice.repository.ReservaRepository;
import com.lealtixservice.service.ReservaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;

    public ReservaServiceImpl(ReservaRepository reservaRepository, MesaRepository mesaRepository) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaDTO> listByTenant(Long tenantId) {
        return reservaRepository.findByTenantIdOrderByFechaDesc(tenantId).stream()
                .map(r -> ReservaDTO.fromEntity(r, resolveMesaNombre(r)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaDTO getById(Long id, Long tenantId) {
        Reserva reserva = getReserva(id, tenantId);
        return ReservaDTO.fromEntity(reserva, resolveMesaNombre(reserva));
    }

    @Override
    @Transactional
    public ReservaDTO create(Long tenantId, ReservaRequest request) {
        if (request.getClienteNombre() == null || request.getClienteNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es requerido");
        }
        if (request.getFecha() == null) {
            throw new IllegalArgumentException("La fecha de la reservación es requerida");
        }
        validateMesaIfPresent(tenantId, request.getMesaId());

        Reserva reserva = Reserva.builder()
                .tenantId(tenantId)
                .clienteNombre(request.getClienteNombre().trim())
                .telefono(request.getTelefono())
                .fecha(request.getFecha())
                .numeroPersonas(request.getNumeroPersonas() != null ? request.getNumeroPersonas() : 1)
                .mesaId(request.getMesaId())
                .estado(request.getEstado() != null ? request.getEstado() : ReservaEstado.PENDIENTE)
                .notas(request.getNotas())
                .build();
        reserva = reservaRepository.save(reserva);
        return ReservaDTO.fromEntity(reserva, resolveMesaNombre(reserva));
    }

    @Override
    @Transactional
    public ReservaDTO update(Long id, Long tenantId, ReservaRequest request) {
        Reserva reserva = getReserva(id, tenantId);
        if (request.getClienteNombre() != null && !request.getClienteNombre().isBlank()) {
            reserva.setClienteNombre(request.getClienteNombre().trim());
        }
        if (request.getTelefono() != null) {
            reserva.setTelefono(request.getTelefono());
        }
        if (request.getFecha() != null) {
            reserva.setFecha(request.getFecha());
        }
        if (request.getNumeroPersonas() != null) {
            reserva.setNumeroPersonas(request.getNumeroPersonas());
        }
        validateMesaIfPresent(tenantId, request.getMesaId());
        reserva.setMesaId(request.getMesaId());
        if (request.getEstado() != null) {
            reserva.setEstado(request.getEstado());
        }
        if (request.getNotas() != null) {
            reserva.setNotas(request.getNotas());
        }
        reserva = reservaRepository.save(reserva);
        return ReservaDTO.fromEntity(reserva, resolveMesaNombre(reserva));
    }

    @Override
    @Transactional
    public ReservaDTO cancel(Long id, Long tenantId) {
        Reserva reserva = getReserva(id, tenantId);
        if (reserva.getEstado() == ReservaEstado.CANCELADA) {
            throw new IllegalArgumentException("La reservación ya está cancelada");
        }
        reserva.setEstado(ReservaEstado.CANCELADA);
        reserva = reservaRepository.save(reserva);
        return ReservaDTO.fromEntity(reserva, resolveMesaNombre(reserva));
    }

    @Override
    @Transactional
    public void delete(Long id, Long tenantId) {
        Reserva reserva = getReserva(id, tenantId);
        reservaRepository.delete(reserva);
    }

    private Reserva getReserva(Long id, Long tenantId) {
        return reservaRepository.findById(id)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Reservación no encontrada para este local"));
    }

    private void validateMesaIfPresent(Long tenantId, Long mesaId) {
        if (mesaId == null) {
            return;
        }
        Mesa mesa = mesaRepository.findById(mesaId)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("La mesa asignada no existe en este local"));
    }

    private String resolveMesaNombre(Reserva reserva) {
        if (reserva.getMesaId() == null) {
            return null;
        }
        return mesaRepository.findById(reserva.getMesaId())
                .map(Mesa::getNombre)
                .orElse(null);
    }
}