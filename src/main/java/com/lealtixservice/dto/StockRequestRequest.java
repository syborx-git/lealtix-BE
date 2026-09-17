package com.lealtixservice.dto;

import lombok.Data;

@Data
public class StockRequestRequest {

    private Long tenantId;

    /** Id del insumo solicitado; si llega null se usa el nombre libre */
    private Long insumoId;

    private String insumoNombre;

    /** Área que solicita: COCINA | BARRA */
    private String area;

    private Double cantidad;

    /** Prioridad: ALTA | MEDIA | BAJA (default MEDIA) */
    private String prioridad;
}