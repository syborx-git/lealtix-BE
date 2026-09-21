package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del reporte general de ventas/comandas (JOIN client_order + mesa +
 * app_user + tenant_customer). El campo cliente puede ser null: el front lo
 * muestra como "Cliente no registrado".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportRowDTO {
    private String folio;
    private LocalDateTime horarioApertura;
    private LocalDateTime horarioCierre;
    private String mesa;
    private String mesero;
    private BigDecimal totalPagado;
    private String cliente;
}