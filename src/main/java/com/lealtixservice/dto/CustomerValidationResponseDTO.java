package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para la validación de cliente en el ChatBot.
 * Incluye información completa del cliente, LTV, cupones activos y sugerencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResponseDTO {

    private boolean exists;

    private TenantCustomerDTO customer;

    private BigDecimal ltv;  // Customer Lifetime Value

    private Integer orderCount;  // Total de órdenes

    private List<CouponResponseDTO> activeCoupons;

    private List<ProductSuggestionDTO> frequentProducts;  // Productos frecuentes

    private List<ProductSuggestionDTO> lastOrderProducts;  // "Lo de siempre" - última orden

    /**
     * DTO simplificado para sugerencias de productos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestionDTO {
        private Long productId;
        private String productName;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Integer quantity;  // Para "lo de siempre"
        private String comments;   // Comentarios del último pedido
    }
}
