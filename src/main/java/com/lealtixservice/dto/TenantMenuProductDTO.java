package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMenuProductDTO {
    private Long id;
    private  Long categoryId;
    private String categoryName;
    private String categoryDescription;
    private Boolean categoryIsActive;
    private Integer categoryDisplayOrder;
    private Long tenantId;
    private String name;
    private String description;
    private Boolean isActive;
    private BigDecimal price;
    private String imageUrl;
    private List<CrossSellingDTO> crossSellingProducts;
}