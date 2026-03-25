package com.lealtixservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO específico para eventos SSE de nuevas órdenes.
 * Estructura optimizada para el frontend con todos los campos requeridos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSseEventDTO {

    @JsonProperty("type")
    private String type; // Siempre "new-order"

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonProperty("meta")
    private MetaDTO meta;

    @JsonProperty("order")
    private OrderDataDTO order;

    /**
     * Metadata del evento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaDTO {
        @JsonProperty("origin")
        private String origin; // "CHATBOT"
    }

    /**
     * Datos completos de la orden
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDataDTO {
        @JsonProperty("id")
        private String id; // UUID como string

        @JsonProperty("tenantId")
        private Long tenantId;

        @JsonProperty("customerId")
        private Long customerId; // null si es venta general

        @JsonProperty("customerName")
        private String customerName; // null si es venta general

        @JsonProperty("estado")
        private String estado; // "PENDING", "PAID", "CANCELLED" (inglés para FE)

        @JsonProperty("source")
        private String source; // "CHATBOT"

        @JsonProperty("subtotal")
        private BigDecimal subtotal;

        @JsonProperty("descuento")
        private BigDecimal descuento;

        @JsonProperty("total")
        private BigDecimal total;

        @JsonProperty("items")
        private List<OrderItemDataDTO> items;

        @JsonProperty("couponCode")
        private String couponCode; // null si no hay cupón

        @JsonProperty("couponId")
        private String couponId; // null si no hay cupón (Long como string para consistencia)

        @JsonProperty("couponDiscount")
        private BigDecimal couponDiscount; // 0 si no hay cupón

        @JsonProperty("fecha")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant fecha;
    }

    /**
     * Datos de item de la orden
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDataDTO {
        @JsonProperty("productId")
        private Long productId;

        @JsonProperty("productName")
        private String productName;

        @JsonProperty("cantidad")
        private Integer cantidad;

        @JsonProperty("precioUnitario")
        private BigDecimal precioUnitario;

        @JsonProperty("comentarios")
        private String comentarios; // null si no hay comentarios
    }
}
