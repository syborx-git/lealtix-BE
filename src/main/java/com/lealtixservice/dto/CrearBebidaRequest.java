package com.lealtixservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Creación transaccional de una bebida (detalle en {@code bebida} + receta en {@code bebida_receta}). */
@Data
public class CrearBebidaRequest {

    private Long tenantId;
    private String nombre;
    private String descripcion;
    private BigDecimal precioVenta;
    private String tipoBebida;         // 'directa' | 'preparada'

    /** Bebidas directas: insumo físico que se descuenta 1:1 por pieza vendida (opcional). */
    private Long insumoId;

    private Double stockMinimo;

    /** Bebidas preparadas: insumos requeridos por 1 pieza (obligatorio). */
    private List<RecetaLinea> receta = new ArrayList<>();

    @Data
    public static class RecetaLinea {
        private Long insumoId;
        private Double cantidad;
    }
}