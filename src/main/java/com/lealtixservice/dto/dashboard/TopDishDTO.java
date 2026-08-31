package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para productos más pedidos en el dashboard de cocina.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDishDTO {
    private Long productId;
    private String productName;
    private Long quantity;
    private BigDecimal totalSales;
    private Integer rank;
}
