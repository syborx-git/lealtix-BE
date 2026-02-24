package com.lealtixservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignTemplateDTO {
    private Long id;
    private String name;
    private String category;
    private String defaultTitle;
    private String defaultSubtitle;
    private String defaultDescription;
    private String defaultImageUrl;
    private String defaultPromoType;
    private Boolean active;
    private Boolean inUse;
}