package com.lealtixservice.dto;

import lombok.*;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigDTO {
    private Long id;
    private Long tenantId;
    private String history;
    private String vision;
    private String bussinesEmail;
    private String twitter;
    private String facebook;
    private String linkedin;
    private String instagram;
    private String tiktok;
    private boolean SMisActive;
    private Boolean kitchenModuleEnabled;
    private LocalDateTime kitchenEnabledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

