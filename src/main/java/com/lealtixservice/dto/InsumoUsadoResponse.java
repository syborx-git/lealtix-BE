package com.lealtixservice.dto;

import lombok.Data;

/** Insumo/producto consumido por una comanda, candidato a registrarse como merma. */
@Data
public class InsumoUsadoResponse {

    private Long insumoId;
    private String insumoNombre;
    private Long productoId;
    private String productoNombre;
    private Double cantidad;
    private String unidad;
    private Double costoUnitario;
    private Double costoTotal;
}