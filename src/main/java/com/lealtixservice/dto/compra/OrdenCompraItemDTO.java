package com.lealtixservice.dto.compra;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraItemDTO {
    private Long id;
    private Long insumoId;
    private String insumoNombre;
    private String areaDestino;
    private String unidad;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal cantidadSugerida;
    private BigDecimal cantidadComprada;
    private BigDecimal costoUnitarioEstimado;
    private BigDecimal costoUnitarioReal;
    private BigDecimal subtotalEstimado;
    private BigDecimal subtotalReal;
    private Boolean surtido;
}

