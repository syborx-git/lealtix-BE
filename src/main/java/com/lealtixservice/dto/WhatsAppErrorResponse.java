package com.lealtixservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Respuesta de error de WhatsApp API")
public class WhatsAppErrorResponse {

    @Schema(description = "Objeto de error")
    private ErrorDetail error;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {
        @Schema(description = "Mensaje de error")
        private String message;

        @Schema(description = "Código de error de Meta")
        private Integer code;

        @Schema(description = "Tipo de error")
        private String type;

        @Schema(description = "Parámetro que causó el error")
        private String error_user_title;

        @Schema(description = "Descripción del error")
        private String error_user_msg;

        @Schema(description = "Detalles adicionales del error")
        private List<ErrorUserData> error_data;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorUserData {
        private String description;
    }
}
