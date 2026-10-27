package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Representa un artículo o platillo ordenado en la comanda con sus insumos detallados
 * para la selección de merma completa o parcial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaItemMermaDTO {

    private UUID itemId;
    private Long productoId;
    private String productoNombre;
    private String categoria;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Double costoEstimado;
    private Boolean tieneReceta;
    private List<InsumoUsadoResponse> insumos;
}
