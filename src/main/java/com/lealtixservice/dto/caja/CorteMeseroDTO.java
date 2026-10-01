package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorteMeseroDTO {
    private Long idMesero;
    private String nombreMesero;
    private Long totalComandasAtendidas;
    private BigDecimal totalVentas;
    private BigDecimal totalPropinas;
    private BigDecimal propinasPendientesLiquidar;
    private List<PagoDTO> pagosRealizados;
    private List<DesgloseMetodoPagoDTO> desgloseMetodos;
}
