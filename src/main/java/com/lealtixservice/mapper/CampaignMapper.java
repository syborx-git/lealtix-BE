package com.lealtixservice.mapper;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.CampaignResult;
import com.lealtixservice.entity.CampaignTemplate;
import com.lealtixservice.entity.PromotionReward;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.enums.PromoType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CampaignMapper {

    public static Campaign toEntity(CreateCampaignRequest request, CampaignTemplate template) {
        if (request == null) return null;
        return Campaign.builder()
                .template(template)
                .businessId(request.getBusinessId())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                // NOTA: promoType y promoValue removidos - usar PromotionReward
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(CampaignStatus.DRAFT)
                .callToAction(request.getCallToAction())
                .channels(listToCommaSeparated(request.getChannels()))
                .segmentation(listToCommaSeparated(request.getSegmentation()))
                .isAutomatic(Optional.ofNullable(request.getIsAutomatic()).orElse(false))
                .build();
    }

    public static CampaignTemplate toTemplateEntity(CampaignTemplateDTO dto) {
        if (dto == null) return null;
        return CampaignTemplate.builder()
                .id(dto.getId())
                .name(dto.getName())
                .category(dto.getCategory())
                .defaultTitle(dto.getDefaultTitle())
                .defaultSubtitle(dto.getDefaultSubtitle())
                .defaultDescription(dto.getDefaultDescription())
                .defaultImageUrl(dto.getDefaultImageUrl())
                .defaultPromoType(parsePromoType(dto.getDefaultPromoType()))
                .isActive(dto.getActive())
                .build();
    }

    public static void updateTemplateEntity(CampaignTemplateDTO dto, CampaignTemplate entity) {
        if (dto == null || entity == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        if (dto.getDefaultTitle() != null) entity.setDefaultTitle(dto.getDefaultTitle());
        if (dto.getDefaultSubtitle() != null) entity.setDefaultSubtitle(dto.getDefaultSubtitle());
        if (dto.getDefaultDescription() != null) entity.setDefaultDescription(dto.getDefaultDescription());
        if (dto.getDefaultImageUrl() != null) entity.setDefaultImageUrl(dto.getDefaultImageUrl());
        if (dto.getDefaultPromoType() != null) entity.setDefaultPromoType(parsePromoType(dto.getDefaultPromoType()));
        if (dto.getActive() != null) entity.setIsActive(dto.getActive());
    }

    public static void updateEntityFromRequest(UpdateCampaignRequest request, Campaign entity) {
        if (request == null || entity == null) return;
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getSubtitle() != null) entity.setSubtitle(request.getSubtitle());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getImageUrl() != null) entity.setImageUrl(request.getImageUrl());
        if (request.getPromoType() != null) {
            entity.setPromoType(parsePromoType(request.getPromoType()));
        }
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        // Usar setStatus personalizado que sincroniza isDraft automáticamente
        if (request.getStatus() != null) {
            CampaignStatus status = parseStatus(request.getStatus());
            if (status != null) {
                entity.setStatus(status);
            }
        }
        if (request.getCallToAction() != null) entity.setCallToAction(request.getCallToAction());
        if (request.getChannels() != null) entity.setChannels(listToCommaSeparated(request.getChannels()));
        if (request.getSegmentation() != null) entity.setSegmentation(listToCommaSeparated(request.getSegmentation()));
        if (request.getIsAutomatic() != null) entity.setIsAutomatic(request.getIsAutomatic());
    }

    public static CampaignResponse toResponse(Campaign campaign) {
        if (campaign == null) return null;
        CampaignTemplateDTO templateDTO = null;
        if (campaign.getTemplate() != null) {
            templateDTO = toTemplateDTO(campaign.getTemplate());
        }
        List<String> channelsList = commaSeparatedToList(campaign.getChannels());

        // Construir el CampaignResponse con el builder
        // (no retornar aquí, porque necesitamos setear promotionReward después si existe)

        CampaignResponse response = CampaignResponse.builder()
                .id(campaign.getId())
                .template(templateDTO)
                .businessId(campaign.getBusinessId())
                .title(campaign.getTitle())
                .subtitle(campaign.getSubtitle())
                .description(campaign.getDescription())
                .imageUrl(campaign.getImageUrl())
                .promoType(enumToString(campaign.getPromoType()))
                .promoValue(null)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(determineStatus(campaign))
                .callToAction(campaign.getCallToAction())
                .channels(channelsList)
                .segmentation(campaign.getSegmentation())
                .isAutomatic(campaign.getIsAutomatic())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .inUse(isActiveNow(campaign))
                .build();

        if (campaign.getPromotionReward() != null) {
            response.setPromotionReward(mapPromotionReward(campaign.getPromotionReward()));
        }
        return response;
    }

    public static CampaignTemplateDTO toTemplateDTO(CampaignTemplate template) {
        if (template == null) return null;
        return CampaignTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .category(template.getCategory())
                .defaultTitle(template.getDefaultTitle())
                .defaultSubtitle(template.getDefaultSubtitle())
                .defaultDescription(template.getDefaultDescription())
                .defaultImageUrl(template.getDefaultImageUrl())
                .defaultPromoType(enumToString(template.getDefaultPromoType()))
                .active(template.getIsActive())
                .build();
    }

    public static CampaignResultDTO toResultDTO(CampaignResult result) {
        if (result == null) return null;
        return CampaignResultDTO.builder()
                .id(result.getId())
                .campaignId(result.getCampaign() != null ? result.getCampaign().getId() : null)
                .views(result.getViews())
                .clicks(result.getClicks())
                .redemptions(result.getRedemptions())
                .lastViewAt(result.getLastViewAt())
                .lastRedemptionAt(result.getLastRedemptionAt())
                .build();
    }

    private static PromoType parsePromoType(String value) {
        if (value == null) return null;
        try {
            return PromoType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // podría lanzar excepción custom si se desea
        }
    }

    private static CampaignStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return CampaignStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String enumToString(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static String determineStatus(Campaign campaign) {
        if (campaign == null) return null;
        Boolean isDraft = null;
        try {
            isDraft = campaign.getIsDraft();
        } catch (Exception e) {
            // ignore if getter not present
        }
        if (isDraft != null) {
            return isDraft ? CampaignStatus.DRAFT.name() : CampaignStatus.ACTIVE.name();
        }
        // Fallback to enum status if available
        return enumToString(campaign.getStatus());
    }

    private static String listToCommaSeparated(List<String> list) {
        return list == null ? null : list.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(","));
    }

    private static List<String> commaSeparatedToList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(value.split(","));
    }

    // Mapeo auxiliar para PromotionReward -> PromotionRewardResponse
    private static PromotionRewardResponse mapPromotionReward(PromotionReward reward) {
        if (reward == null) return null;
        return PromotionRewardResponse.builder()
                .id(reward.getId())
                .campaignId(reward.getCampaign() != null ? reward.getCampaign().getId() : null)
                .rewardType(reward.getRewardType())
                .numericValue(reward.getNumericValue())
                .productId(reward.getProductId())
                .buyQuantity(reward.getBuyQuantity())
                .freeQuantity(reward.getFreeQuantity())
                .customConfig(reward.getCustomConfig())
                .description(reward.getDescription())
                .minPurchaseAmount(reward.getMinPurchaseAmount())
                .usageLimit(reward.getUsageLimit())
                .usageCount(reward.getUsageCount())
                .createdAt(reward.getCreatedAt())
                .updatedAt(reward.getUpdatedAt())
                .build();
    }

    private static boolean isActiveNow(Campaign campaign) {
        if (campaign == null) return false;
        String status = determineStatus(campaign);
        if (!CampaignStatus.ACTIVE.name().equals(status)) return false;
        LocalDate today = LocalDate.now();
        LocalDate start = campaign.getStartDate();
        LocalDate end = campaign.getEndDate();
        boolean afterStart = start == null || !today.isBefore(start);
        boolean beforeEnd = end == null || !today.isAfter(end);
        return afterStart && beforeEnd;
    }
}