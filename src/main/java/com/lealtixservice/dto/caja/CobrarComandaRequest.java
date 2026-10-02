package com.lealtixservice.dto.caja;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CobrarComandaRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotNull(message = "cajeroId es requerido")
    private Long cajeroId;

    @NotBlank(message = "metodoPago es requerido")
    private String metodoPago;

    @NotNull(message = "montoCuenta es requerido")
    @DecimalMin(value = "0.0", message = "El monto de la cuenta no puede ser negativo")
    private BigDecimal montoCuenta;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "El monto de propina no puede ser negativo")
    private BigDecimal montoPropina = BigDecimal.ZERO;

    private String referencia;
}
