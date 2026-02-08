package com.lealtixservice.entity;

import com.lealtixservice.enums.CampaignEmailStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_email", indexes = {
        @Index(name = "idx_campaign_email_campaign", columnList = "campaign_id"),
        @Index(name = "idx_campaign_email_status", columnList = "status"),
        @Index(name = "idx_campaign_email_recipient", columnList = "recipient_email"),
        @Index(name = "idx_campaign_email_provider_msg", columnList = "provider_message_id")
})
@Getter
@Setter
@ToString(exclude = {"campaign"})
@EqualsAndHashCode(exclude = {"campaign"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotBlank
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignEmailStatus status;

    @Builder.Default
    @NotNull
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "provider_error_code", length = 100)
    private String providerErrorCode;

    @Column(name = "provider_error_message", columnDefinition = "TEXT")
    private String providerErrorMessage;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CampaignEmailStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
