package com.lealtixservice.dto;

import com.lealtixservice.entity.Mesa;
import com.lealtixservice.enums.MesaEstado;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaDTO {
    private Long id;
    private Long tenantId;
    private String nombre;
    private Integer numero;
    private Integer capacidad;
    private MesaEstado estado;
    private Long meseroUserId;
    private String meseroNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MesaDTO fromEntity(Mesa mesa, String meseroNombre) {
        return MesaDTO.builder()
                .id(mesa.getId())
                .tenantId(mesa.getTenantId())
                .nombre(mesa.getNombre())
                .numero(mesa.getNumero())
                .capacidad(mesa.getCapacidad())
                .estado(mesa.getEstado())
                .meseroUserId(mesa.getMeseroUserId())
                .meseroNombre(meseroNombre)
                .createdAt(mesa.getCreatedAt())
                .updatedAt(mesa.getUpdatedAt())
                .build();
    }
}