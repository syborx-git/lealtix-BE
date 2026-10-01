package com.lealtixservice.dto.caja;

import jakarta.validation.constraints.DecimalMax;
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
public class LiquidarPropinasRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotNull(message = "idTurno es requerido")
    private Long idTurno;

    @NotNull(message = "idMesero es requerido")
    private Long idMesero;

    @NotNull(message = "idCajero es requerido")
    private Long idCajero;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "La retención no puede ser menor a 0%")
    @DecimalMax(value = "100.0", message = "La retención no puede exceder el 100%")
    private BigDecimal porcentajeRetencion = BigDecimal.ZERO;
}
