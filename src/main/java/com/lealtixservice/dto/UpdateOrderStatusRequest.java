package com.lealtixservice.dto;

import com.lealtixservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para cambiar el estado de una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    
    @NotNull(message = "estado es requerido")
    private OrderStatus estado;
    
    private String userEmail;  // Email del usuario que realiza el cambio (para auditoría)
    
    private String reason;     // Razón del cambio de estado (ej: "Cliente canceló", "Error en preparación")
}
