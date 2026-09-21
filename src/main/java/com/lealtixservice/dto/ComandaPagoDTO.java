package com.lealtixservice.dto;

import com.lealtixservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de una sub-comanda de pago por asiento (dividida de una comanda original).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaPagoDTO {
    private UUID id;
    private UUID orderId;
    private UUID seatId;
    private String seatAlias;
    private String folio;
    private String folioOriginal;
    private BigDecimal total;
    private String estado;
    private PaymentMethod paidMethod;
    private String paymentReference;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}