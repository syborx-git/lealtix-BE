package com.lealtixservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para crear una orden desde el ChatBot.
 * Extiende la funcionalidad de CreateClientOrderRequest con campos específicos del ChatBot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotOrderRequestDTO {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotBlank(message = "sessionId es requerido")
    private String sessionId;

    private Long customerId;  // Nullable si es venta general

    private String customerPhone;  // Para registro rápido

    private String customerEmail;  // Para registro rápido

    private String customerName;  // Para registro rápido

    @NotNull(message = "items es requerido")
    @Valid
    private List<OrderItemRequest> items;

    private BigDecimal descuento;

    private BigDecimal subtotal;

    private BigDecimal totalFinal;

    private String couponCode;

    @Builder.Default
    private String source = "CHATBOT";

    /**
     * DTO anidado para los items de la orden
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "productId es requerido")
        private Long productId;

        @NotNull(message = "cantidad es requerida")
        private Integer cantidad;

        @NotNull(message = "precioUnitario es requerido")
        private BigDecimal precioUnitario;

        private String comentarios;
    }
}
