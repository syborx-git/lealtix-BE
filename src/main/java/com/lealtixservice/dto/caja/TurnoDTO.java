package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoDTO {
    private Long idTurno;
    private Long tenantId;
    private Long idCajero;
    private String nombreCajero;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal fondoInicial;
    private BigDecimal totalIngresos;
    private BigDecimal totalPropinas;
    private BigDecimal totalEfectivoDeclarado;
    private BigDecimal diferenciaCaja;
    private String estado;
    private String observaciones;
}
