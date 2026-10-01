package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesgloseMetodoPagoDTO {
    private String metodoPago;
    private Long transacciones;
    private BigDecimal totalCuenta;
    private BigDecimal totalPropina;
    private BigDecimal totalRecaudado;
}
