package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;
import com.lealtixservice.enums.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaign", indexes = {
        @Index(name = "idx_campaign_business", columnList = "business_id"),
        @Index(name = "idx_campaign_status", columnList = "status"),
        @Index(name = "idx_campaign_start_date", columnList = "start_date"),
        @Index(name = "idx_campaign_business_draft", columnList = "business_id,is_draft")
})
@Getter
@Setter
@ToString(exclude = {"template", "result", "promotionReward", "coupons"})
@EqualsAndHashCode(exclude = {"template", "result", "promotionReward", "coupons"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CampaignTemplate template; // nullable

    @NotNull
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @NotBlank
    @Column(length = 200)
    private String title;

    @Column(length = 200)
    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;


    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CampaignStatus status; // draft, active, inactive, scheduled

    @Column(name = "call_to_action", length = 200)
    private String callToAction;

    @Column(length = 500)
    private String channels; // email, whatsapp, etc (comma separated)

    @Column(columnDefinition = "TEXT")
    private String segmentation; // JSON string opcional (mantener compatibilidad)

    @Builder.Default
    @NotNull
    @Column(name = "is_automatic")
    private Boolean isAutomatic = false;

    @Builder.Default
    @NotNull
    @Column(name = "is_draft")
    private Boolean isDraft = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "total_sent")
    private Integer totalSent = 0;

    @Builder.Default
    @Column(name = "total_failed")
    private Integer totalFailed = 0;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToOne(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CampaignResult result;

    @OneToOne(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private PromotionReward promotionReward;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Coupon> coupons = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Establece el status de la campaña y sincroniza el flag isDraft.
     * Mantiene consistencia entre status y isDraft automáticamente.
     */
    public void setStatus(CampaignStatus status) {
        this.status = status;
        if (status != null) {
            this.isDraft = (status == CampaignStatus.DRAFT);
        }
    }

    /**
     * Establece el resultado de la campaña manteniendo consistencia bidireccional.
     * @param result El resultado a asociar
     */
    public void setResultBidirectional(CampaignResult result) {
        this.result = result;
        if (result != null) {
            result.setCampaign(this);
        }
    }

    /**
     * Establece el reward de la campaña manteniendo consistencia bidireccional.
     * @param reward El reward a asociar
     */
    public void setPromotionRewardBidirectional(PromotionReward reward) {
        this.promotionReward = reward;
        if (reward != null) {
            reward.setCampaign(this);
        }
    }
}