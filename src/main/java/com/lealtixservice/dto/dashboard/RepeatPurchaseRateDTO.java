package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepeatPurchaseRateDTO {
    private Long totalCustomers;
    private Long repeatCustomers;
    private BigDecimal repeatRate;
    private Long oneTimeBuyers;
    private Long multiTimeBuyers;
}
