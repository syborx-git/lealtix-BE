package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaCajaRowDTO {
    private UUID id;
    private String folioComanda;
    private String estado;
    private Long idMesa;
    private String mesaNombre;
    private Long idMesero;
    private String meseroNombre;
    private String clienteNombre;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private Integer totalItems;
    private LocalDateTime horaApertura;
    private LocalDateTime fechaImpresionTicket;
    private Boolean propinasLiquidadas;
}
