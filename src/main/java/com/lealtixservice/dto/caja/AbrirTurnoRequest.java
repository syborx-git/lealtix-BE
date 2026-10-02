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
public class AbrirTurnoRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotNull(message = "cajeroId es requerido")
    private Long cajeroId;

    @NotNull(message = "fondoInicial es requerido")
    @DecimalMin(value = "0.0", message = "El fondo inicial no puede ser negativo")
    private BigDecimal fondoInicial;

    private String observaciones;
}
