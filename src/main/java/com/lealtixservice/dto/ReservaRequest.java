package com.lealtixservice.dto;

import com.lealtixservice.enums.ReservaEstado;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequest {
    private String clienteNombre;
    private String telefono;
    private LocalDateTime fecha;
    private Integer numeroPersonas;
    private Long mesaId;
    private ReservaEstado estado;
    private String notas;
}