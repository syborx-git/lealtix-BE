package com.lealtixservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para interacciones del ChatBot.
 * Representa un mensaje o acción en la conversación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotInteractionDTO {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotBlank(message = "sessionId es requerido")
    private String sessionId;

    private Long customerId;  // Nullable hasta que se identifique

    @NotBlank(message = "message es requerido")
    private String message;

    private String messageType;  // TEXT, COMMAND, PRODUCT_QUERY, etc.

    private Map<String, Object> context;  // Contexto adicional (productId, quantity, etc.)
}
