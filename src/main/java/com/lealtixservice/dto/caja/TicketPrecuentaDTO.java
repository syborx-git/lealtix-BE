package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPrecuentaDTO {
    private UUID idComanda;
    private String folioComanda;
    private String mesaNombre;
    private String meseroNombre;
    private String clienteNombre;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaImpresion;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private BigDecimal propinaSugerida10;
    private BigDecimal propinaSugerida15;
    private BigDecimal propinaSugerida20;
    private List<ItemPrecuentaDTO> items;
}
