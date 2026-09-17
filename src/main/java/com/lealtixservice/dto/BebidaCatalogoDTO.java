package com.lealtixservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Bebida lista para el POS: incluye stock disponible calculado según su tipo. */
@Data
public class BebidaCatalogoDTO {

    private Long id;
    private Long tenantId;
    private String nombre;
    private String descripcion;
    private BigDecimal precioVenta;
    private String tipoBebida;   // 'directa' | 'preparada'
    private String unidad;       // 'pieza'

    /** Piezas vendibles: directa = stock físico (barra) del insumo enlazado; preparada = min(ratios de receta). */
    private Double piezasDisponibles;
    private boolean disponible;

    /** Solo para bebidas 'preparadas': desglose por ingrediente (stock de barra vs requerido). */
    private List<IngredienteCatalogoDTO> ingredientes = new ArrayList<>();

    @Data
    public static class IngredienteCatalogoDTO {
        private Long insumoId;
        private String insumoNombre;
        private String unidad;
        private Double cantidadRequerida;
        private Double stockEnBarra;
        private Double piezasPosibles;
    }
}