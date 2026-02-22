package com.lealtixservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long id;
    private CampaignTemplateDTO template;
    private Long businessId;
    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;
    private String promoType;
    private String promoValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String callToAction;
    private List<String> channels;
    private String segmentation;
    private Boolean isAutomatic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PromotionRewardResponse promotionReward;
    private Boolean inUse;
}