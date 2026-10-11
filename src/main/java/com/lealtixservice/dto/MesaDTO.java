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
    private String identificador;
    private Integer numero;
    private Integer capacidad;
    private MesaEstado estado;
    private Long meseroUserId;
    private String meseroNombre;
    private Double posicionX;
    private Double posicionY;
    private Double ancho;
    private Double alto;
    private MesaForma forma;
    private Integer rotacion;
    private String idGrupoTemporal;
    private String identificadorUnico;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MesaDTO fromEntity(Mesa mesa, String meseroNombre) {
        String iden = mesa.getIdentificador() != null && !mesa.getIdentificador().isBlank()
                ? mesa.getIdentificador()
                : (mesa.getNumero() != null ? String.valueOf(mesa.getNumero()) : (mesa.getId() != null ? String.valueOf(mesa.getId()) : ""));

        return MesaDTO.builder()
                .id(mesa.getId())
                .tenantId(mesa.getTenantId())
                .nombre(mesa.getNombre())
                .identificador(iden)
                .numero(mesa.getNumero())
                .capacidad(mesa.getCapacidad())
                .estado(mesa.getEstado())
                .meseroUserId(mesa.getMeseroUserId())
                .meseroNombre(meseroNombre)
                .posicionX(mesa.getPosicionX())
                .posicionY(mesa.getPosicionY())
                .ancho(mesa.getAncho() != null ? mesa.getAncho() : 80.0)
                .alto(mesa.getAlto() != null ? mesa.getAlto() : 80.0)
                .forma(mesa.getForma())
                .rotacion(mesa.getRotacion())
                .idGrupoTemporal(mesa.getIdGrupoTemporal())
                .identificadorUnico(iden)
                .createdAt(mesa.getCreatedAt())
                .updatedAt(mesa.getUpdatedAt())
                .build();
    }
}