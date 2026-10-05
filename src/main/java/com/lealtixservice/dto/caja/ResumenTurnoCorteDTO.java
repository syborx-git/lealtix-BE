package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenTurnoCorteDTO {
    private TurnoDTO turno;
    private Long totalArticulosVendidos;
    private Long totalComandasCobradas;
    private BigDecimal totalVentas;
    private BigDecimal totalCuenta;
    private BigDecimal totalPropinas;
    private BigDecimal totalRecaudado;
    private BigDecimal fondoInicial;
    private BigDecimal efectivoEsperadoEnCaja;
    private List<DesgloseMetodoPagoDTO> desgloseMetodos;
    private LocalDate fechaCorte;
}
