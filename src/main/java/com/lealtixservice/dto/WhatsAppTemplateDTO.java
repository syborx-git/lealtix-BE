package com.lealtixservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Datos de la plantilla WhatsApp")
public class WhatsAppTemplateDTO {

    @Schema(description = "Nombre de la plantilla", example = "hello_world")
    private String name;

    @Schema(description = "Código de idioma", example = "es_MX")
    private String languageCode;

    @Schema(description = "Parámetros de la plantilla")
    private TemplateParameters parameters;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TemplateParameters {
        @Schema(description = "Body parameters del template")
        private TemplateParameter[] body;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TemplateParameter {
        @Schema(description = "Valor del parámetro")
        private String type;

        @Schema(description = "Contenido del parámetro")
        private String text;
    }
}
