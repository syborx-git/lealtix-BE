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
@Schema(description = "Respuesta del envío de mensaje WhatsApp")
public class WhatsAppMessageResponse {

    @Schema(description = "ID del mensaje enviado", example = "wamid.HBEUGoH6AxcYnAX_2YrTBk9YbAV")
    private String messageId;

    @Schema(description = "Estado del mensaje", example = "sent")
    private String status;

    @Schema(description = "Contacto destinatario", example = "+525571866433")
    private String recipient;

    @Schema(description = "Timestamp del envío")
    private Long timestamp;
}
