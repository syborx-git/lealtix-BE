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
public class IdentifiedVsGeneralSalesDTO {
    private Long identifiedOrdersCount;
    private BigDecimal identifiedRevenue;
    private BigDecimal identifiedAvgTicket;
    
    private Long generalOrdersCount;
    private BigDecimal generalRevenue;
    private BigDecimal generalAvgTicket;
    
    private BigDecimal identifiedPercentage;
    private BigDecimal generalPercentage;
}
