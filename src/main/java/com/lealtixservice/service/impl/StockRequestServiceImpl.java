package com.lealtixservice.service.impl;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.StockRequestRequest;
import com.lealtixservice.dto.StockRequestResponse;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.StockRequest;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.StockRequestRepository;
import com.lealtixservice.service.InventoryService;
import com.lealtixservice.service.StockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockRequestServiceImpl implements StockRequestService {

    private static final Set<String> AREAS = Set.of("COCINA", "BARRA");
    private static final Set<String> PRIORIDADES = Set.of("ALTA", "MEDIA", "BAJA");

    private final StockRequestRepository stockRequestRepository;
    private final InsumoRepository insumoRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public GenericResponse crearSolicitud(StockRequestRequest request) {
        if (request == null || request.getTenantId() == null) {
            throw new BusinessRuleException("El tenant es requerido");
        }
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new BusinessRuleException("La cantidad solicitada debe ser mayor a 0");
        }

        String area = request.getArea() != null ? request.getArea().trim().toUpperCase() : null;
        if (area == null || !AREAS.contains(area)) {
            throw new BusinessRuleException("El área solicitante debe ser COCINA o BARRA");
        }

        String prioridad = request.getPrioridad() != null ? request.getPrioridad().trim().toUpperCase() : "MEDIA";
        if (!PRIORIDADES.contains(prioridad)) {
            throw new BusinessRuleException("La prioridad debe ser ALTA, MEDIA o BAJA");
        }

        String nombre = request.getInsumoNombre() != null && !request.getInsumoNombre().isBlank()
                ? request.getInsumoNombre().trim()
                : null;

        Long insumoId = request.getInsumoId();
        if (insumoId != null) {
            Insumo insumo = insumoRepository.findById(insumoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado: " + insumoId));
            if (!request.getTenantId().equals(insumo.getTenantId())) {
                throw new BusinessRuleException("El insumo no pertenece al tenant indicado");
            }
            nombre = insumo.getNombre();
        }

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("Indica el insumo que deseas solicitar");
        }

        StockRequest guardada = stockRequestRepository.save(StockRequest.builder()
                .tenantId(request.getTenantId())
                .insumoId(insumoId)
                .insumoNombre(nombre)
                .area(area)
                .cantidad(request.getCantidad())
                .prioridad(prioridad)
                .estado("PENDIENTE")
                .build());

        return new GenericResponse(200, "Solicitud de stock enviada", toResponse(guardada));
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse listarPorTenant(Long tenantId) {
        List<StockRequestResponse> lista = stockRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new GenericResponse(200, "Solicitudes obtenidas", lista);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse contarPendientesPorArea(Long tenantId) {
        Map<String, Long> conteos = new LinkedHashMap<>();
        conteos.put("cocina", stockRequestRepository.countByTenantIdAndEstadoAndArea(tenantId, "PENDIENTE", "COCINA"));
        conteos.put("barra", stockRequestRepository.countByTenantIdAndEstadoAndArea(tenantId, "PENDIENTE", "BARRA"));
        return new GenericResponse(200, "Solicitudes pendientes por área", conteos);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse listarPendientesDetalle(Long tenantId, String area) {
        List<StockRequestResponse> lista;
        if (area != null && !area.isBlank()) {
            String a = area.trim().toUpperCase();
            if (!AREAS.contains(a)) {
                throw new BusinessRuleException("El área debe ser COCINA o BARRA");
            }
            lista = stockRequestRepository.findByTenantIdAndEstadoAndAreaOrderByCreatedAtDesc(tenantId, "PENDIENTE", a).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            lista = stockRequestRepository.findByTenantIdAndEstadoOrderByCreatedAtDesc(tenantId, "PENDIENTE").stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return new GenericResponse(200, "Solicitudes pendientes", lista);
    }

    @Override
    @Transactional
    public GenericResponse aceptarSolicitud(Long id, Long tenantId) {
        StockRequest solicitud = stockRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + id));
        if (!solicitud.getTenantId().equals(tenantId)) {
            throw new BusinessRuleException("La solicitud no pertenece al tenant indicado");
        }
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new BusinessRuleException("La solicitud ya fue procesada");
        }
        if (solicitud.getInsumoId() == null) {
            throw new BusinessRuleException("El insumo vinculado a la solicitud ya no existe");
        }

        String destino = "COCINA".equals(solicitud.getArea()) ? "cocina" : "barra";
        GenericResponse mov = inventoryService.moverBodega(solicitud.getInsumoId(), solicitud.getCantidad(), destino);
        if (mov.getCode() != 200) {
            throw new BusinessRuleException(mov.getMessage());
        }

        solicitud.setEstado("SURTIDO");
        StockRequest procesada = stockRequestRepository.save(solicitud);
        return new GenericResponse(200,
                "Transferencia completada: " + solicitud.getInsumoNombre() + " enviado a " + solicitud.getArea(),
                toResponse(procesada));
    }

    private StockRequestResponse toResponse(StockRequest s) {
        StockRequestResponse r = new StockRequestResponse();
        r.setId(s.getId());
        r.setTenantId(s.getTenantId());
        r.setInsumoId(s.getInsumoId());
        r.setInsumoNombre(s.getInsumoNombre());
        r.setArea(s.getArea());
        r.setCantidad(s.getCantidad());
        r.setPrioridad(s.getPrioridad());
        r.setEstado(s.getEstado());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}