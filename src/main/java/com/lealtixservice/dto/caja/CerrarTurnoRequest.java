package com.lealtixservice.dto.caja;

import jakarta.validation.constraints.DecimalMin;
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
public class CerrarTurnoRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotNull(message = "idTurno es requerido")
    private Long idTurno;

    @NotNull(message = "totalEfectivoDeclarado es requerido")
    @DecimalMin(value = "0.0", message = "El efectivo declarado no puede ser negativo")
    private BigDecimal totalEfectivoDeclarado;

    private String observaciones;
}
