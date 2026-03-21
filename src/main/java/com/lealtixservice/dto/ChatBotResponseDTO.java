package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta genérica del ChatBot.
 * Incluye el mensaje de respuesta y datos adicionales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotResponseDTO {

    private String sessionId;

    private String message;

    private String messageType;  // TEXT, PRODUCT_LIST, ORDER_CONFIRMATION, ERROR

    private Object data;  // Datos adicionales (productos, orden, etc.)

    private String nextAction;  // Sugerencia de siguiente acción

    private boolean requiresInput;  // ¿Requiere input del usuario?
}
