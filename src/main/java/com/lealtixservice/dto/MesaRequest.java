package com.lealtixservice.dto;

import com.lealtixservice.enums.MesaEstado;
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
}