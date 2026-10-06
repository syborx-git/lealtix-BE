package com.lealtixservice.dto;

import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para respuesta completa de una orden con todos sus detalles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderDTO {
    private UUID id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long tenantId;
    private LocalDateTime fecha;
    private OrderStatus estado;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private List<ClientOrderItemDTO> items;
    private String source;  // Origen: 'CHATBOT', 'MANUAL', 'POS', 'WEB', 'MOBILE'
    private String couponCode;  // Código del cupón aplicado (ej: "SUMMER20")
    private Long couponId;  // ID del cupón aplicado
    private BigDecimal couponDiscount;  // Descuento específico del cupón
    private LocalDateTime acceptedAt;  // Timestamp cuando fue aceptado en cocina
    private LocalDateTime readyAt;  // Timestamp cuando estuvo listo
    private OrderStatus barraEstado;
    private OrderStatus cocinaEstado;
    private LocalDateTime barraReadyAt;
    private LocalDateTime cocinaReadyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PaymentMethod paidMethod;  // Método de pago: CASH, CARD, TRANSFER, MIXED
    private String paymentReference;  // Referencia del comprobante
    private String paidByName;  // Nombre del usuario que registró el pago
    private LocalDateTime paidAt;  // Timestamp cuando se registró el pago

    /**
     * Propina que se cobro por encima de la cuenta. Viaja aparte de {@code total}
     * porque no es ingreso de la comanda: el corte de caja la reporta aparte.
     */
    private BigDecimal propina;
    private Boolean propinasLiquidadas;
    private LocalDateTime fechaLiquidacionPropinas;
    
    private Long mesaId;
    private String mesaNombre;
    private Integer mesaNumero;
    private Long meseroId;
    private String meseroNombre;
    private Long idTurno;
    private LocalDateTime horaApertura;

    // Campos de cancelación
    private String cancelledBy;  // Email del usuario que canceló
    private LocalDateTime cancelledAt;  // Timestamp de cancelación
    private String cancellationReason;  // Razón de la cancelación
}
