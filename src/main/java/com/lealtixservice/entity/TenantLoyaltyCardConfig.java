package com.lealtixservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_loyalty_card_config", indexes = {
        @Index(name = "idx_loyalty_config_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@ToString(exclude = {"tenant", "rewardProduct"})
@EqualsAndHashCode(exclude = {"tenant", "rewardProduct"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantLoyaltyCardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    @NotNull
    private Tenant tenant;

    @Column(name = "card_title", length = 150, nullable = false)
    private String cardTitle;

    @Column(name = "card_subtitle", length = 255)
    private String cardSubtitle;

    @Column(name = "bg_color_hex", length = 20, nullable = false)
    private String bgColorHex;

    @Column(name = "text_color_hex", length = 20, nullable = false)
    private String textColorHex;

    @Column(name = "accent_color_hex", length = 20, nullable = false)
    private String accentColorHex;

    @Column(name = "stamp_icon", length = 50, nullable = false)
    private String stampIcon;

    @Column(name = "total_stamps", nullable = false)
    private Integer totalStamps;

    @Column(name = "reward_mode", length = 50, nullable = false)
    private String rewardMode; // FULL_CARD | MILESTONE_PER_STAMP

    @Column(name = "reward_type", length = 50, nullable = false)
    private String rewardType; // FREE_PRODUCT | PERCENT_DISCOUNT | FIXED_AMOUNT | TWO_FOR_ONE | CUSTOM

    @Column(name = "reward_value", precision = 10, scale = 2)
    private BigDecimal rewardValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_product_id")
    private TenantMenuProduct rewardProduct;

    @Column(name = "reward_title", length = 200, nullable = false)
    private String rewardTitle;

    @Column(name = "reward_description", columnDefinition = "TEXT")
    private String rewardDescription;

    @Column(name = "milestone_config", columnDefinition = "TEXT")
    private String milestoneConfig;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.cardTitle == null) this.cardTitle = "Tarjeta de Fidelización";
        if (this.bgColorHex == null) this.bgColorHex = "#4338CA";
        if (this.textColorHex == null) this.textColorHex = "#FFFFFF";
        if (this.accentColorHex == null) this.accentColorHex = "#F59E0B";
        if (this.stampIcon == null) this.stampIcon = "HAMBURGER";
        if (this.totalStamps == null) this.totalStamps = 8;
        if (this.rewardMode == null) this.rewardMode = "FULL_CARD";
        if (this.rewardType == null) this.rewardType = "FREE_PRODUCT";
        if (this.rewardTitle == null) this.rewardTitle = "Recompensa de Cortesía";
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

