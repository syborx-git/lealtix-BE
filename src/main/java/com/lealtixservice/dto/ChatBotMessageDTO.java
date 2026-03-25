package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para mensajes del ChatBot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotMessageDTO {

    private Long id;

    private String messageType;

    private String sender;

    private String content;

    private String metadata;

    private LocalDateTime timestamp;
}
