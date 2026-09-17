package com.lealtixservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockRequestResponse {

    private Long id;
    private Long tenantId;
    private Long insumoId;
    private String insumoNombre;
    private String area;
    private Double cantidad;
    private String prioridad;
    private String estado;
    private LocalDateTime createdAt;
}