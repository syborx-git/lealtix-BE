package com.lealtixservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_stamp_history", indexes = {
        @Index(name = "idx_loyalty_stamp_history_card", columnList = "card_id"),
        @Index(name = "idx_loyalty_stamp_history_tenant_created", columnList = "tenant_id,created_at"),
        @Index(name = "idx_loyalty_stamp_history_customer", columnList = "customer_id")
})
@Getter
@Setter
@ToString(exclude = {"card", "tenant", "customer", "order", "registeredByUser"})
@EqualsAndHashCode(exclude = {"card", "tenant", "customer", "order", "registeredByUser"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyStampHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    @NotNull
    private CustomerLoyaltyCard card;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    private TenantCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ClientOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_user_id")
    private AppUser registeredByUser;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType; // ADD_STAMP | REDEEM_REWARD | RESET_CYCLE | MANUAL_ADJUST

    @Column(name = "stamps_change", nullable = false)
    private Integer stampsChange;

    @Column(name = "stamps_before", nullable = false)
    private Integer stampsBefore;

    @Column(name = "stamps_after", nullable = false)
    private Integer stampsAfter;

    @Column(name = "reward_redeemed", length = 255)
    private String rewardRedeemed;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

