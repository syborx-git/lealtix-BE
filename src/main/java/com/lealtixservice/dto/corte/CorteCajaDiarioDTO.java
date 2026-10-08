package com.lealtixservice.dto.corte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorteCajaDiarioDTO {
    private Long idCorteDiario;
    private Long tenantId;
    private Long cajeroId;
    private String cajeroNombre;
    private String cajeroEmail;
    private LocalDate fechaCorte;
    private LocalDateTime fechaHoraRegistro;

    private BigDecimal sistemaEfectivo;
    private BigDecimal sistemaTarjeta;
    private BigDecimal sistemaTransferencia;
    private BigDecimal sistemaOtros;
    private BigDecimal sistemaTotal;

    private BigDecimal realEfectivo;
    private BigDecimal realTarjeta;
    private BigDecimal realTransferencia;
    private BigDecimal realOtros;
    private BigDecimal realTotal;

    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTarjeta;
    private BigDecimal diferenciaTransferencia;
    private BigDecimal diferenciaOtros;
    private BigDecimal diferenciaTotal;

    private String estadoDiferencia;
    private String comentarios;
    private Long totalComandas;
    private Long totalArticulos;
    private BigDecimal totalPropinas;
}
