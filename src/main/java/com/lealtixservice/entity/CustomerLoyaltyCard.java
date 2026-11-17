package com.lealtixservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_loyalty_card", indexes = {
        @Index(name = "idx_customer_loyalty_card_tenant_customer", columnList = "tenant_id,customer_id"),
        @Index(name = "idx_customer_loyalty_card_stamps", columnList = "tenant_id,current_stamps"),
        @Index(name = "idx_customer_loyalty_card_pending_reward", columnList = "tenant_id,has_pending_reward")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_customer_loyalty_card", columnNames = {"tenant_id", "customer_id"})
})
@Getter
@Setter
@ToString(exclude = {"tenant", "customer"})
@EqualsAndHashCode(exclude = {"tenant", "customer"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLoyaltyCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    private TenantCustomer customer;

    @Column(name = "current_stamps", nullable = false)
    private Integer currentStamps;

    @Column(name = "completed_cards_count", nullable = false)
    private Integer completedCardsCount;

    @Column(name = "total_stamps_accumulated", nullable = false)
    private Integer totalStampsAccumulated;

    @Column(name = "has_pending_reward", nullable = false)
    private Boolean hasPendingReward;

    @Column(name = "pending_reward_description", length = 255)
    private String pendingRewardDescription;

    @Column(name = "google_wallet_object_id", length = 255)
    private String googleWalletObjectId;

    @Column(name = "google_wallet_save_url", columnDefinition = "TEXT")
    private String googleWalletSaveUrl;

    @Column(name = "last_stamp_at")
    private LocalDateTime lastStampAt;

    @Column(name = "last_redemption_at")
    private LocalDateTime lastRedemptionAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.currentStamps == null) this.currentStamps = 0;
        if (this.completedCardsCount == null) this.completedCardsCount = 0;
        if (this.totalStampsAccumulated == null) this.totalStampsAccumulated = 0;
        if (this.hasPendingReward == null) this.hasPendingReward = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

