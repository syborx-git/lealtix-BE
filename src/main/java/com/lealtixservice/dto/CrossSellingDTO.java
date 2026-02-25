package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellingDTO {
    
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private Long categoryId;
    private String categoryName;
    
    /**
     * Constructor simplificado para proyecciones desde queries.
     */
    public CrossSellingDTO(Long id, String name, BigDecimal price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}
