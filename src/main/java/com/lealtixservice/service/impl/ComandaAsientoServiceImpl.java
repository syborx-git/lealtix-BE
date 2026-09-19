package com.lealtixservice.service.impl;

import com.lealtixservice.dto.AddSeatRequest;
import com.lealtixservice.dto.AssignItemsRequest;
import com.lealtixservice.dto.ComandaAsientoDTO;
import com.lealtixservice.dto.UpdateSeatAliasRequest;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.ComandaAsiento;
import com.lealtixservice.entity.ComandaAsientoItem;
import com.lealtixservice.enums.ComandaAsientoEstado;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.ComandaAsientoItemRepository;
import com.lealtixservice.repository.ComandaAsientoRepository;
import com.lealtixservice.service.ComandaAsientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ComandaAsientoServiceImpl implements ComandaAsientoService {

    private final ComandaAsientoRepository comandaAsientoRepository;
    private final ComandaAsientoItemRepository comandaAsientoItemRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComandaAsientoDTO> listSeats(UUID orderId) {
        getOrderOrThrow(orderId);
        return comandaAsientoRepository.findByOrderIdOrderByNumeroAsc(orderId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ComandaAsientoDTO addSeat(UUID orderId, AddSeatRequest request) {
        ClientOrder order = getOrderOrThrow(orderId);

        ComandaAsiento seat = ComandaAsiento.builder()
                .order(order)
                .tenantId(order.getTenant().getId())
                .numero(resolveNextNumero(orderId, request.getNumero()))
                .alias(request.getAlias() != null && !request.getAlias().isBlank() ? request.getAlias().trim() : null)
                .estado(ComandaAsientoEstado.ABIERTA)
                .total(BigDecimal.ZERO)
                .build();

        seat = comandaAsientoRepository.save(seat);
        log.info("Asiento {} añadido a la comanda {}", seat.getNumero(), orderId);
        return toDTO(seat);
    }

    @Override
    public ComandaAsientoDTO renameSeat(UUID seatId, UpdateSeatAliasRequest request) {
        ComandaAsiento seat = getSeatOrThrow(seatId);
        seat.setAlias(request.getAlias() != null ? request.getAlias().trim() : null);
        seat = comandaAsientoRepository.save(seat);
        log.info("Alias del asiento {} actualizado a '{}'", seatId, seat.getAlias());
        return toDTO(seat);
    }

    @Override
    public List<ComandaAsientoDTO> assignItems(UUID seatId, AssignItemsRequest request) {
        ComandaAsiento seat = getSeatOrThrow(seatId);
        UUID orderId = seat.getOrder().getId();

        if (seat.getEstado() == ComandaAsientoEstado.PAGADA) {
            throw new IllegalArgumentException("No se pueden asignar ítems a un asiento ya pagado");
        }

        for (UUID itemId : request.getItemIds()) {
            if (itemId == null) {
                throw new IllegalArgumentException("itemId es requerido");
            }
            ClientOrderItem item = clientOrderItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ítem de comanda no encontrado con ID: " + itemId));
            if (!item.getOrder().getId().equals(orderId)) {
                throw new IllegalArgumentException("El ítem " + itemId + " no pertenece a la comanda " + orderId);
            }
            if (comandaAsientoItemRepository.findByItemId(itemId).isPresent()) {
                throw new IllegalArgumentException("El ítem " + itemId + " ya está asignado a un asiento");
            }
            comandaAsientoItemRepository.save(ComandaAsientoItem.builder()
                    .seat(seat)
                    .item(item)
                    .build());
        }

        recalculateSeatTotal(seat);
        log.info("Ítems asignados al asiento {} de la comanda {}. Nuevo total: {}", seatId, orderId, seat.getTotal());
        return listSeats(orderId);
    }

    @Override
    public void deleteSeat(UUID seatId) {
        ComandaAsiento seat = getSeatOrThrow(seatId);
        UUID orderId = seat.getOrder().getId();
        comandaAsientoItemRepository.deleteBySeatId(seatId);
        comandaAsientoRepository.delete(seat);
        log.info("Asiento {} eliminado de la comanda {}", seatId, orderId);
    }

    /**
     * Determina el número del siguiente asiento: el indicado en el request
     * (si es libre) o el siguiente disponible.
     */
    private Integer resolveNextNumero(UUID orderId, Integer requestedNumero) {
        if (requestedNumero != null && requestedNumero > 0) {
            return requestedNumero;
        }
        return comandaAsientoRepository.findFirstByOrderIdOrderByNumeroDesc(orderId)
                .map(seat -> seat.getNumero() + 1)
                .orElse(1);
    }

    /**
     * Recalcula el total del asiento con base en sus ítems asignados.
     */
    private void recalculateSeatTotal(ComandaAsiento seat) {
        BigDecimal total = comandaAsientoItemRepository.findBySeatId(seat.getId()).stream()
                .map(asientoItem -> asientoItem.getItem().getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(
                                asientoItem.getItem().getCantidad() != null ? asientoItem.getItem().getCantidad() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        seat.setTotal(total);
        comandaAsientoRepository.save(seat);
    }

    private ClientOrder getOrderOrThrow(UUID orderId) {
        return clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
    }

    private ComandaAsiento getSeatOrThrow(UUID seatId) {
        return comandaAsientoRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado con ID: " + seatId));
    }

    private ComandaAsientoDTO toDTO(ComandaAsiento seat) {
        return ComandaAsientoDTO.builder()
                .id(seat.getId())
                .orderId(seat.getOrder() != null ? seat.getOrder().getId() : null)
                .tenantId(seat.getTenantId())
                .numero(seat.getNumero())
                .alias(seat.getAlias())
                .estado(seat.getEstado())
                .total(seat.getTotal())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}