package com.lealtixservice.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para añadir un asiento/persona a una comanda.
 * El alias (nombre de la persona) es opcional; el número se asigna de forma
 * automática (siguiente libre) si no se indica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSeatRequest {

    private String alias;

    @Positive(message = "numero debe ser mayor a 0")
    private Integer numero;
}