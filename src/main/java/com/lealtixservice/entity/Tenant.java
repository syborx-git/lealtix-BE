package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"appUser"})
@EqualsAndHashCode(exclude = {"appUser"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreNegocio;
    private String direccion;
    private String telefono;
    private String tipoNegocio;
    private String slug;
    private String UIDTenant;
    private String schedules;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private AppUser appUser;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "kitchen_module_enabled")
    private Boolean kitchenModuleEnabled;

    @Column(name = "kitchen_enabled_at")
    private LocalDateTime kitchenEnabledAt;

    @Builder.Default
    private boolean isActive = true;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
