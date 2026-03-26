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
        
        return ClientOrder.builder()
                .customer(customer)
                .tenant(tenant)
                .estado(OrderStatus.PENDIENTE)
                .subtotal(subtotal)
                .descuento(descuento)
                .total(total)
                .couponId(request.getCouponId())
                .fecha(java.time.LocalDateTime.now())  // Establecer la fecha actual
                .source(request.getSource() != null ? request.getSource() : "MANUAL")  // Soporte para source
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
