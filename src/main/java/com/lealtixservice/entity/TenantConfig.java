package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_config")
public class TenantConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

   @Column(length = 500)
    private String history;
    @Column(length = 500)
    private String vision;
    private String bussinesEmail;
    private String twitter;
    private String facebook;
    private String linkedin;
    private String instagram;
    private String tiktok;
    private String schedules;

    @Builder.Default
    @Column(name = "kitchen_module_enabled", nullable = false)
    private Boolean kitchenModuleEnabled = false;

    @Column(name = "kitchen_enabled_at")
    private java.time.LocalDateTime kitchenEnabledAt;

    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
