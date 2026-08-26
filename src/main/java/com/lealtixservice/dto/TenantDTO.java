package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la entidad Tenant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {
    private Long id;
    private Long userId;
    private String nombreNegocio;
    private String direccion;
    private String telefono;
    private String tipoNegocio;
    private String slug;
    private String logoUrl;
    private String slogan;
    private String UIDTenant;
    private String email;
    private String bussinessEmail;
    private boolean isActive;
    private String history;
    private String vision;
    private String facebook;
    private String instagram;
    private String linkedin;
    private String x;
    private String tiktok;
    private String schedules;
    private Boolean kitchenModuleEnabled;
    private LocalDateTime kitchenEnabledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

