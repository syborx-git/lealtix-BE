package com.lealtixservice.mapper;

import com.lealtixservice.dto.ClientOrderItemDTO;
import com.lealtixservice.dto.CreateClientOrderRequest.OrderItemRequest;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.TenantMenuProduct;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper para convertir entre ClientOrderItem y ClientOrderItemDTO
 */
public class ClientOrderItemMapper {

    /**
     * Convierte un OrderItemRequest a una entidad ClientOrderItem
     */
    public static ClientOrderItem toEntity(OrderItemRequest request, ClientOrder order, TenantMenuProduct product) {
        if (request == null) return null;
        
        LocalDateTime now = LocalDateTime.now();
        
        // Usar el precio del request si está disponible, sino usar el del producto
        BigDecimal precioUnitario = request.getPrecioUnitario() != null ? request.getPrecioUnitario() : 
                                    (product != null ? product.getPrecio() : BigDecimal.ZERO);
        
        return ClientOrderItem.builder()
                .order(order)
                .product(product)
                .cantidad(request.getCantidad())
                .precioUnitario(precioUnitario)
                .comentarios(request.getComentarios())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Convierte una entidad ClientOrderItem a un DTO ClientOrderItemDTO
     */
    public static ClientOrderItemDTO toDTO(ClientOrderItem item) {
        if (item == null) return null;
        
        // Calcular subtotal del item
        BigDecimal subtotal = BigDecimal.ZERO;
        if (item.getPrecioUnitario() != null && item.getCantidad() != null) {
            subtotal = item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad()));
        }
        
        return ClientOrderItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrder() != null ? item.getOrder().getId() : null)
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getNombre() : null)
                .productDescription(item.getProduct() != null ? item.getProduct().getDescripcion() : null)
                .productImageUrl(item.getProduct() != null ? item.getProduct().getImgUrl() : null)
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(subtotal)
                .comentarios(item.getComentarios())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    /**
     * Actualiza una entidad ClientOrderItem desde un DTO
     */
    public static void updateEntity(ClientOrderItemDTO dto, ClientOrderItem entity) {
        if (dto == null || entity == null) return;
        
        if (dto.getCantidad() != null) {
            entity.setCantidad(dto.getCantidad());
        }
        if (dto.getPrecioUnitario() != null) {
            entity.setPrecioUnitario(dto.getPrecioUnitario());
        }
        if (dto.getComentarios() != null) {
            entity.setComentarios(dto.getComentarios());
        }
    }
}
