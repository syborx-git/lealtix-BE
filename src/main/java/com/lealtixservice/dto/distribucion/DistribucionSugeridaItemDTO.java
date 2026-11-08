package com.lealtixservice.dto.distribucion;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionSugeridaItemDTO {
    private Long insumoId;
    private String insumoNombre;
    private String areaDestino; // 'COCINA' o 'BARRA'
    private String unidad;
    private Double stockAreaActual;
    private Double stockMinimo;
    private Double stockBodega;
    private Double cantidadSugerida;
    private Double faltanteTotal;
    private String tipoDisponibilidad; // 'COMPLETO', 'PARCIAL', 'AGOTADO_EN_BODEGA'
}

