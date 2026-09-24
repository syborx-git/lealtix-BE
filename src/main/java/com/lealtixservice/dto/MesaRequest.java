package com.lealtixservice.dto;

import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.MesaForma;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaRequest {
    private String nombre;
    private Integer numero;
    private Integer capacidad;
    private MesaEstado estado;
    private Long meseroUserId;
    private Double posicionX;
    private Double posicionY;
    private MesaForma forma;
    private Integer rotacion;
}