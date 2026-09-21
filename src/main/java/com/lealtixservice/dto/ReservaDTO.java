package com.lealtixservice.dto;

import com.lealtixservice.entity.Reserva;
import com.lealtixservice.enums.ReservaEstado;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDTO {
    private Long id;
    private Long tenantId;
    private String clienteNombre;
    private String telefono;
    private LocalDateTime fecha;
    private Integer numeroPersonas;
    private Long mesaId;
    private String mesaNombre;
    private ReservaEstado estado;
    private String notas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReservaDTO fromEntity(Reserva reserva, String mesaNombre) {
        return ReservaDTO.builder()
                .id(reserva.getId())
                .tenantId(reserva.getTenantId())
                .clienteNombre(reserva.getClienteNombre())
                .telefono(reserva.getTelefono())
                .fecha(reserva.getFecha())
                .numeroPersonas(reserva.getNumeroPersonas())
                .mesaId(reserva.getMesaId())
                .mesaNombre(mesaNombre)
                .estado(reserva.getEstado())
                .notas(reserva.getNotas())
                .createdAt(reserva.getCreatedAt())
                .updatedAt(reserva.getUpdatedAt())
                .build();
    }
}