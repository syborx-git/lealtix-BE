package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLoyaltyCardDto {
    private Long id;
    private Long tenantId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer currentStamps;
    private Integer totalStampsRequired;
    private Integer completedCardsCount;
    private Integer totalStampsAccumulated;
    private Boolean hasPendingReward;
    private String pendingRewardDescription;
    private String googleWalletObjectId;
    private String googleWalletSaveUrl;
    private LocalDateTime lastStampAt;
    private LocalDateTime lastRedemptionAt;
}

