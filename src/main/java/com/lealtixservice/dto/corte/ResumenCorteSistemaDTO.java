package com.lealtixservice.dto.corte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCorteSistemaDTO {
    private LocalDate fecha;
    private BigDecimal sistemaEfectivo;
    private BigDecimal sistemaTarjeta;
    private BigDecimal sistemaTransferencia;
    private BigDecimal sistemaOtros;
    private BigDecimal sistemaTotal;
    private Long totalComandas;
    private Long totalArticulos;
    private BigDecimal totalPropinas;
}
