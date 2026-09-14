package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para un item/detalle de una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderItemDTO {
    private UUID id;
    private UUID orderId;
    private Long productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal; // cantidad * precioUnitario
    private String comentarios;
    private List<Long> excludedIngredientIds;   // Ingredientes modificables que el cliente pidió quitar
    private List<Long> additionalIngredientIds; // Insumos adicionales seleccionados por el cliente
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
