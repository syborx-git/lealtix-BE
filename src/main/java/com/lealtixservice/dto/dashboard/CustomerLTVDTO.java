package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLTVDTO {
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal lifetimeValue;
    private Long totalOrders;
    private BigDecimal averageOrderValue;
    private LocalDateTime firstPurchase;
    private LocalDateTime lastPurchase;
}
