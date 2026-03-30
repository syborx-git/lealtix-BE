package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para productos de venta cruzada (cross-sell) del dashboard mesero.
 * Contiene información de productos trending para sugerir al cliente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossSellProductDTO {
    
    /**
     * ID del producto
     */
    private Long id;
    
    /**
     * Nombre del producto
     */
    private String name;
    
    /**
     * Precio del producto
     */
    private BigDecimal price;
    
    /**
     * URL de imagen del producto
     */
    private String imageUrl;
    
    /**
     * Categoría del producto
     */
    private String category;
    
    /**
     * Rol/perfil al cual se sugiere este producto
     */
    private String suggestedFor;
    
    /**
     * Stock disponible
     */
    private Long stock;
}
