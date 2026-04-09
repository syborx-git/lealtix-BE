package com.lealtixservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Solicitud para enviar un mensaje de plantilla WhatsApp")
public class WhatsAppMessageRequest {

    @NotBlank(message = "El número de teléfono es requerido")
    @Schema(description = "Número de teléfono en formato E.164 (ej: +525571866433)", example = "+525571866433")
    private String phoneNumber;

    @NotBlank(message = "El nombre de la plantilla es requerido")
    @Schema(description = "Nombre de la plantilla configurada en WhatsApp", example = "hello_world")
    private String templateName;

    @Schema(description = "Código de idioma ISO (ej: en_US, es_MX)", example = "es_MX")
    private String languageCode;

    @Schema(description = "Variables dinámicas para la plantilla (parámetros en formato array)")
    private String[] templateParameters;
}
