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
public class CampaignROIDTO {
    private Long campaignId;
    private String campaignName;
    private BigDecimal campaignCost;
    private BigDecimal revenueGenerated;
    private BigDecimal profit;
    private BigDecimal roi;
    private Long ordersCount;
}
