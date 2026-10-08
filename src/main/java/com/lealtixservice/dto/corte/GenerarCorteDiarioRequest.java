package com.lealtixservice.dto.corte;

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
public class GenerarCorteDiarioRequest {

    @NotNull(message = "El tenantId es obligatorio")
    private Long tenantId;

    private BigDecimal realEfectivo;
    private BigDecimal realTarjeta;
    private BigDecimal realTransferencia;
    private BigDecimal realOtros;

    private String comentarios;
}
