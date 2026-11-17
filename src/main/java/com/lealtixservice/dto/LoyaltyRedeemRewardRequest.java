package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyRedeemRewardRequest {
    private Long tenantId;
    private Long customerId;
    private UUID orderId;
    private Long userId;
    private String notes;
}

