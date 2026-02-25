package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para crear o actualizar configuraciones de cross-selling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCrossSellingRequest {
    
    @NotNull(message = "El ID del producto principal es requerido")
    private Long productId;
    
    @NotNull(message = "El ID del producto sugerido es requerido")
    private Long suggestedProductId;
    
    @NotNull(message = "El ID del tenant es requerido")
    private Long tenantId;
    
    @Positive(message = "El orden de visualización debe ser positivo")
    private Integer displayOrder = 1;
    
    private Boolean isActive = true;
}
