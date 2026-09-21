package com.lealtixservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MermaResponse {

    private Long id;
    private Long tenantId;
    private String ticket;
    private UUID orderId;
    private UUID registroId;
    private String tipoMerma;
    private String categoriaMerma;
    private String origen;
    private String motivo;
    private Long usuarioId;
    private String usuarioNombre;
    private Long insumoId;
    private String insumoNombre;
    private Long productoId;
    private String productoNombre;
    private Double cantidad;
    private String unidad;
    private Double costoUnitario;
    private Double costoTotal;
    private LocalDateTime fecha;
}