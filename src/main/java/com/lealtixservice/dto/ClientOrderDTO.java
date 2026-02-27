package com.lealtixservice.dto;

import com.lealtixservice.enums.OrderStatus;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
