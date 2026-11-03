package com.lealtixservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarMesaRequest {

    @NotNull(message = "El ID de la nueva mesa es obligatorio")
    private Long nuevaMesaId;
}
