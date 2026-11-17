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
public class LoyaltyCardConfigDto {
    private Long id;
    private Long tenantId;
    private String cardTitle;
    private String cardSubtitle;
    private String bgColorHex;
    private String textColorHex;
    private String accentColorHex;
    private String stampIcon;
    private Integer totalStamps;
    private String rewardMode; // FULL_CARD | MILESTONE_PER_STAMP
    private String rewardType; // FREE_PRODUCT | PERCENT_DISCOUNT | FIXED_AMOUNT | TWO_FOR_ONE | CUSTOM
    private BigDecimal rewardValue;
    private Long rewardProductId;
    private String rewardProductName;
    private String rewardTitle;
    private String rewardDescription;
    private String termsAndConditions;
    private Boolean active;
    private String tenantName;
    private String tenantLogoUrl;
}

