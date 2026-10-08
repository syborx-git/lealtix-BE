package com.lealtixservice.dto.corte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCorteDiarioHoyDTO {
    private boolean yaGenerado;
    private CorteCajaDiarioDTO corte;
    private ResumenCorteSistemaDTO sistemaActual;
}
