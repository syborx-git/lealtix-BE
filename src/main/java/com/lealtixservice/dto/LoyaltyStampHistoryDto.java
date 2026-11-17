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
public class LoyaltyStampHistoryDto {
    private Long id;
    private Long cardId;
    private Long customerId;
    private String customerName;
    private String actionType;
    private Integer stampsChange;
    private Integer stampsBefore;
    private Integer stampsAfter;
    private String rewardRedeemed;
    private String notes;
    private String registeredByName;
    private LocalDateTime createdAt;
}

