package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import com.lealtixservice.enums.RedemptionChannel;
import org.springframework.lang.Nullable;

/**
 * DTO para crear una nueva orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientOrderRequest {
    
    @Nullable
    private Long customerId;
    
    @NotNull(message = "tenantId es requerido")
    private Long tenantId;
    
    @NotNull(message = "items es requerido")
    private List<OrderItemRequest> items;
    
    private BigDecimal descuento;

    private BigDecimal subtotal;
    private BigDecimal totalFinal;

    private String couponCode;

    private String redeemedBy;

    private RedemptionChannel redemptionChannel;
    
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
        @Positive(message = "cantidad debe ser mayor a 0")
        private Integer cantidad;
        
        @NotNull(message = "precioUnitario es requerido")
        @Positive(message = "precioUnitario debe ser mayor a 0")
        private BigDecimal precioUnitario;
        
        private String comentarios;
    }
}