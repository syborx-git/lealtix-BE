package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de configuraciones de cross-selling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCrossSellingResponse {
    
    private Long id;
    private Long productId;
    private String productName;
    private Long suggestedProductId;
    private String suggestedProductName;
    private Long tenantId;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
