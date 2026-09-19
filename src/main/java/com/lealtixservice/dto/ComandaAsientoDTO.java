package com.lealtixservice.dto;

import com.lealtixservice.enums.ComandaAsientoEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de un asiento/persona de una comanda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaAsientoDTO {
    private UUID id;
    private UUID orderId;
    private Long tenantId;
    private Integer numero;
    private String alias;
    private ComandaAsientoEstado estado;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}