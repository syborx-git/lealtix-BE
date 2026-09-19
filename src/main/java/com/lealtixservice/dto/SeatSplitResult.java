package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de la división de cuenta por asiento: la comanda original (padre)
 * NO se duplica; por cada asiento cobrado se genera una sub-comanda con
 * folio derivado (ej: 12345-A, 12345-B).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSplitResult {
    private UUID orderId;
    private String folioOriginal;
    private BigDecimal totalPagado;
    private List<ComandaPagoDTO> subComandas;
}