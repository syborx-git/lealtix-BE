package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para mensajes/alertas del dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    
    /**
     * Tipo de mensaje: success, warning, info, error
     */
    private String type;
    
    /**
     * Texto del mensaje
     */
    private String text;
}
