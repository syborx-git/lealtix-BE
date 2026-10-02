package com.lealtixservice.dto;

import com.lealtixservice.entity.Mesa;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.MesaForma;
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
    private Double posicionX;
    private Double posicionY;
    private MesaForma forma;
    private Integer rotacion;
    private String idGrupoTemporal;
    private String identificadorUnico;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MesaDTO fromEntity(Mesa mesa, String meseroNombre) {
        String identificador = mesa.getId() != null
                ? (mesa.getNumero() != null
                    ? String.format("#%d · %s (M-%d)", mesa.getId(), mesa.getNombre(), mesa.getNumero())
                    : String.format("#%d · %s", mesa.getId(), mesa.getNombre()))
                : mesa.getNombre();

        return MesaDTO.builder()
                .id(mesa.getId())
                .tenantId(mesa.getTenantId())
                .nombre(mesa.getNombre())
                .numero(mesa.getNumero())
                .capacidad(mesa.getCapacidad())
                .estado(mesa.getEstado())
                .meseroUserId(mesa.getMeseroUserId())
                .meseroNombre(meseroNombre)
                .posicionX(mesa.getPosicionX())
                .posicionY(mesa.getPosicionY())
                .forma(mesa.getForma())
                .rotacion(mesa.getRotacion())
                .idGrupoTemporal(mesa.getIdGrupoTemporal())
                .identificadorUnico(identificador)
                .createdAt(mesa.getCreatedAt())
                .updatedAt(mesa.getUpdatedAt())
                .build();
    }
}