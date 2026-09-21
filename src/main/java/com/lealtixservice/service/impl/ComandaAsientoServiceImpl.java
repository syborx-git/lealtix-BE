package com.lealtixservice.service.impl;

import com.lealtixservice.dto.AddSeatRequest;
import com.lealtixservice.dto.AssignItemsRequest;
import com.lealtixservice.dto.ComandaAsientoDTO;
import com.lealtixservice.dto.ComandaPagoDTO;
import com.lealtixservice.dto.SeatSettleRequest;
import com.lealtixservice.dto.SeatSplitResult;
import com.lealtixservice.dto.UpdateSeatAliasRequest;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.ComandaAsiento;
import com.lealtixservice.entity.ComandaAsientoItem;
import com.lealtixservice.entity.ComandaPago;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.ComandaAsientoEstado;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.PaymentMethod;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.AppUserRepository;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.ComandaAsientoItemRepository;
import com.lealtixservice.repository.ComandaAsientoRepository;
import com.lealtixservice.repository.ComandaPagoRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.ComandaAsientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final ComandaPagoRepository comandaPagoRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;
    private final AppUserRepository appUserRepository;
    private final TenantUserRepository tenantUserRepository;

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

    @Override
    public SeatSplitResult settleSeats(UUID orderId, SeatSettleRequest request) {
        log.info("Cobrando asientos de la comanda {} con método {}", orderId, request.getMethod());

        // ===== FASE 1: VALIDAR ORDEN =====
        ClientOrder order = getOrderOrThrow(orderId);

        if (request.getTenantId() != null && !order.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("La comanda no pertenece al tenant especificado");
        }
        if (order.getEstado() == OrderStatus.PAGADA || order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se puede cobrar una comanda en estado " + order.getEstado());
        }
        if (order.getPaidAt() != null) {
            throw new IllegalArgumentException("La comanda ya fue pagada el " + order.getPaidAt());
        }

        // ===== FASE 2: VALIDAR USUARIO Y MÉTODO DE PAGO =====
        if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
            throw new IllegalArgumentException("Email del usuario que registra el pago es requerido");
        }
        tenantUserRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado en tenant con email: " + request.getUserEmail()));
        if ((request.getMethod() == PaymentMethod.CARD ||
                request.getMethod() == PaymentMethod.TRANSFER ||
                request.getMethod() == PaymentMethod.MIXED) &&
                (request.getReference() == null || request.getReference().isBlank())) {
            throw new IllegalArgumentException(
                    "Referencia obligatoria para método de pago: " + request.getMethod().getDescription());
        }
        AppUser paidBy = resolvePaidBy(request.getUserEmail());

        // ===== FASE 3: VALIDAR ASIENTOS Y GENERAR SUB-COMANDAS CON FOLIO DERIVADO =====
        List<ComandaAsiento> seats = comandaAsientoRepository.findAllById(request.getSeatIds()).stream()
                .sorted(Comparator.comparing(ComandaAsiento::getNumero))
                .collect(Collectors.toList());

        if (seats.size() != request.getSeatIds().stream().distinct().count()) {
            throw new IllegalArgumentException("Alguno de los asientos indicados no existe");
        }

        String folioOriginal = deriveFolioOriginal(order);
        BigDecimal totalPagado = BigDecimal.ZERO;
        List<ComandaPagoDTO> subComandas = new java.util.ArrayList<>();
        List<ComandaAsiento> aPagar = new java.util.ArrayList<>();

        for (ComandaAsiento seat : seats) {
            if (!seat.getOrder().getId().equals(orderId)) {
                throw new IllegalArgumentException("El asiento " + seat.getId() + " no pertenece a la comanda " + orderId);
            }
            if (seat.getEstado() != ComandaAsientoEstado.ABIERTA) {
                throw new IllegalArgumentException("El asiento " + seat.getNumero() + " ya está en estado " + seat.getEstado());
            }
            if (seat.getTotal() == null || seat.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El asiento " + seat.getNumero() + " no tiene ítems asignados para cobrar");
            }

            ComandaPago pago = ComandaPago.builder()
                    .order(order)
                    .seat(seat)
                    .folio(folioOriginal + "-" + seatSuffix(seat.getNumero()))  // p.ej. 12345-A, 12345-B
                    .folioOriginal(folioOriginal)
                    .total(seat.getTotal())
                    .estado("PAGADA")
                    .paidMethod(request.getMethod())
                    .paymentReference(request.getReference())
                    .paidBy(paidBy)
                    .paidAt(LocalDateTime.now())
                    .build();
            pago = comandaPagoRepository.save(pago);

            seat.setEstado(ComandaAsientoEstado.PAGADA);
            aPagar.add(seat);
            totalPagado = totalPagado.add(seat.getTotal());
            subComandas.add(toPagoDTO(pago));
            log.info("Sub-comanda {} creada por ${} para el asiento {} de la comanda {}",
                    pago.getFolio(), pago.getTotal(), seat.getNumero(), orderId);
        }
        comandaAsientoRepository.saveAll(aPagar);

        // ===== FASE 4: REGISTRAR PAGO EN LA COMANDA ORIGINAL =====
        order.setPaidMethod(request.getMethod());
        order.setPaymentReference(request.getReference());
        order.setPaidBy(paidBy);
        order.setPaidAt(LocalDateTime.now());
        // Si ya no quedan asientos abiertos con ítems, la comanda queda pagada por completo
        boolean quedanAbiertos = comandaAsientoRepository.findByOrderIdOrderByNumeroAsc(orderId).stream()
                .anyMatch(s -> s.getEstado() == ComandaAsientoEstado.ABIERTA
                        && s.getTotal() != null && s.getTotal().compareTo(BigDecimal.ZERO) > 0);
        if (!quedanAbiertos) {
            order.setEstado(OrderStatus.PAGADA);
        }
        clientOrderRepository.save(order);

        return SeatSplitResult.builder()
                .orderId(orderId)
                .folioOriginal(folioOriginal)
                .totalPagado(totalPagado)
                .subComandas(subComandas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComandaPagoDTO> getSubComandas(UUID orderId) {
        getOrderOrThrow(orderId);
        return comandaPagoRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toPagoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Folio de la comanda original. Como client_order no tiene numeración
     * propia, usa el prefijo del UUID de la comanda como folio base; de ahí
     * se derivan los folios de las sub-comandas (ej: ABC12345-A).
     */
    private String deriveFolioOriginal(ClientOrder order) {
        return order.getId().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Sufijo de letra por número de asiento: 1→A, 2→B, ..., 27→AA, 28→AB...
     */
    private String seatSuffix(int numero) {
        StringBuilder sb = new StringBuilder();
        int n = numero;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.length() == 0 ? "A" : sb.toString();
    }

    /**
     * Busca (o crea) el AppUser que registró el pago, siguiendo el patrón de recordPayment.
     */
    private AppUser resolvePaidBy(String userEmail) {
        AppUser paidByUser = appUserRepository.findByEmail(userEmail);
        if (paidByUser == null) {
            TenantUser tenantUser = tenantUserRepository.findByEmail(userEmail).orElse(null);
            paidByUser = AppUser.builder()
                    .email(userEmail)
                    .fullName(tenantUser != null ? tenantUser.getNombre() : userEmail)
                    .isActive(true)
                    .build();
            paidByUser = appUserRepository.save(paidByUser);
        }
        return paidByUser;
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

    private ComandaPagoDTO toPagoDTO(ComandaPago pago) {
        return ComandaPagoDTO.builder()
                .id(pago.getId())
                .orderId(pago.getOrder() != null ? pago.getOrder().getId() : null)
                .seatId(pago.getSeat() != null ? pago.getSeat().getId() : null)
                .seatAlias(pago.getSeat() != null ? pago.getSeat().getAlias() : null)
                .folio(pago.getFolio())
                .folioOriginal(pago.getFolioOriginal())
                .total(pago.getTotal())
                .estado(pago.getEstado())
                .paidMethod(pago.getPaidMethod())
                .paymentReference(pago.getPaymentReference())
                .paidAt(pago.getPaidAt())
                .createdAt(pago.getCreatedAt())
                .build();
    }
}