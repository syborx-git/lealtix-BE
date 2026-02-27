package com.lealtixservice.dto;

import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para Coupon con información completa del reward asociado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDTO {

    private Long id;
    private String code;
    private CouponStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime redeemedAt;
    private String qrToken;
    private String qrUrl;
    private String redeemedBy;
    private String redemptionMetadata;
    private boolean expired;

    // Información de la campaña
    private Long campaignId;
    private String campaignTitle;

    // Información del cliente
    private Long customerId;
    private String customerName;

    // Información del reward (desde promotion_reward)
    private String rewardDescription;
    private BigDecimal minPurchaseAmount;
    private Integer usageLimit;
    private Integer usageCount;
    private RewardType rewardType;
    private BigDecimal numericValue; // Para PERCENT_DISCOUNT o FIXED_AMOUNT
    private Long productId;
    private Integer buyQuantity;
    private Integer freeQuantity;

}

