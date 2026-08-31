package com.lealtixservice.mapper;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.ClientOrderItemDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre ClientOrder, ClientOrderDTO y CreateClientOrderRequest
 */
public class ClientOrderMapper {

    /**
     * Convierte un CreateClientOrderRequest a una entidad ClientOrder
     * Si la orden viene de COMANDIX, el estado inicial es EN_PREPARACION, sino PENDIENTE
     */
    public static ClientOrder toEntity(CreateClientOrderRequest request, TenantCustomer customer, Tenant tenant) {
        if (request == null) return null;
        
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        
        // El subtotal se calculará al agregar items
        BigDecimal total = subtotal.subtract(descuento);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        
        // Validar si la orden viene de COMANDIX - si es así, poner estado EN_PREPARACION
        OrderStatus estado = OrderStatus.PENDIENTE;
        java.time.LocalDateTime acceptedAt = null;
        if (request.getRedemptionChannel() != null && 
            request.getRedemptionChannel().name().equalsIgnoreCase("COMANDIX")) {
            estado = OrderStatus.CONFIRMADA;
            acceptedAt = java.time.LocalDateTime.now();  // Registrar cuándo fue aceptada
        }
        
        // Determinar el source: prioridad a request.getSource() (ej. "CHATBOT"),
        // fallback al canal de redención y por defecto MANUAL.
        String source = request.getSource();
        if (source == null || source.isBlank()) {
            source = request.getRedemptionChannel() != null ? request.getRedemptionChannel().name() : "MANUAL";
        }

        return ClientOrder.builder()
                .customer(customer)
                .tenant(tenant)
                .estado(estado)
                .acceptedAt(acceptedAt)
                .subtotal(subtotal)
                .descuento(descuento)
                .total(total)
                .couponId(request.getCouponId())
                .fecha(java.time.LocalDateTime.now())  // Establecer la fecha actual
                .source(source)
                .build();
    }

    /**
     * Convierte una entidad ClientOrder a un DTO ClientOrderDTO
     */
    public static ClientOrderDTO toDTO(ClientOrder order) {
        return toDTO(order, null, null);
    }

    /**
     * Convierte una entidad ClientOrder a un DTO ClientOrderDTO con información de cupón
     */
    public static ClientOrderDTO toDTO(ClientOrder order, String couponCode, BigDecimal couponDiscount) {
        if (order == null) return null;
        
        List<ClientOrderItemDTO> itemsDTO = null;
        if (order.getItems() != null) {
            itemsDTO = order.getItems().stream()
                    .map(ClientOrderItemMapper::toDTO)
                    .collect(Collectors.toList());
        }
        
        return ClientOrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .customerEmail(order.getCustomer() != null ? order.getCustomer().getEmail() : null)
                .tenantId(order.getTenant() != null ? order.getTenant().getId() : null)
                .fecha(order.getFecha())
                .estado(order.getEstado())
                .subtotal(order.getSubtotal())
                .descuento(order.getDescuento())
                .total(order.getTotal())
                .items(itemsDTO)
                .source(order.getSource())
                .couponCode(couponCode)
                .couponId(order.getCouponId())
                .couponDiscount(couponDiscount != null ? couponDiscount : BigDecimal.ZERO)
                .acceptedAt(order.getAcceptedAt())
                .readyAt(order.getReadyAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paidMethod(order.getPaidMethod())
                .paymentReference(order.getPaymentReference())
                .paidByName(order.getPaidBy() != null ? order.getPaidBy().getEmail() : null)
                .paidAt(order.getPaidAt())
                .cancelledBy(order.getCancelledBy())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }

    /**
     * Actualiza una entidad ClientOrder desde un DTO
     */
    public static void updateEntity(ClientOrderDTO dto, ClientOrder entity) {
        if (dto == null || entity == null) return;
        
        if (dto.getEstado() != null) {
            entity.setEstado(dto.getEstado());
        }
        if (dto.getDescuento() != null) {
            entity.setDescuento(dto.getDescuento());
        }
    }

    /**
     * Calcula el subtotal de una orden basado en sus items
     */
    public static BigDecimal calculateSubtotal(List<ClientOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return items.stream()
                .map(item -> item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total de una orden
     */
    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal descuento) {
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (descuento == null) {
            descuento = BigDecimal.ZERO;
        }
        
        BigDecimal total = subtotal.subtract(descuento);
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }
}
