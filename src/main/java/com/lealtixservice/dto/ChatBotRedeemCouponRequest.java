package com.lealtixservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para solicitar la redención de un cupón desde el ChatBot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotRedeemCouponRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotBlank(message = "couponCode es requerido")
    private String couponCode;

    @NotNull(message = "customerId es requerido")
    private Long customerId;

    @NotNull(message = "orderTotal es requerido")
    private BigDecimal orderTotal; // Total de la orden antes de aplicar el cupón

    // Lista de productos en la orden (para validar 2x1 y productos específicos)
    private List<OrderProductItem> orderProducts;

    // Información adicional
    private String sessionId; // ID de la sesión del ChatBot
    private String metadata; // Información adicional en JSON

    /**
     * DTO para representar un producto en la orden
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderProductItem {
        @NotNull(message = "productId es requerido")
        private Long productId;
        
        @NotBlank(message = "productName es requerido")
        private String productName;
        
        @NotNull(message = "quantity es requerido")
        private Integer quantity;
        
        @NotNull(message = "unitPrice es requerido")
        private BigDecimal unitPrice;
        
        @NotNull(message = "subtotal es requerido")
        private BigDecimal subtotal; // quantity * unitPrice
    }
}
