package com.lealtixservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para renombrar el alias (persona) de un asiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeatAliasRequest {

    @NotBlank(message = "alias es requerido")
    private String alias;
}