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
public class CouponConversionRateDTO {
    private Long campaignId;
    private String campaignName;
    private Long totalCouponsIssued;
    private Long totalCouponsRedeemed;
    private Long ordersWithCoupon;
    private BigDecimal conversionRate;
    private BigDecimal revenueFromCoupons;
}
