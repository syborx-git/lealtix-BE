package com.lealtixservice.entity;

import jakarta.persistence.*;
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
@Table(name = "campaign_email_payload", indexes = {
        @Index(name = "idx_campaign_email_payload_campaign_email", columnList = "campaign_email_id"),
        @Index(name = "idx_campaign_email_payload_created_at", columnList = "created_at")
})
@Getter
@Setter
@ToString(exclude = {"campaignEmail"})
@EqualsAndHashCode(exclude = {"campaignEmail"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignEmailPayload {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_email_id", nullable = false)
    private CampaignEmail campaignEmail;

    @Column(name = "email_subject", length = 255)
    private String emailSubject;

    @Column(name = "email_body", columnDefinition = "TEXT")
    private String emailBody;

    @Column(name = "email_body_html", columnDefinition = "TEXT")
    private String emailBodyHtml;

    @Column(name = "template_id", length = 100)
    private String templateId;

    @Column(name = "template_data", columnDefinition = "TEXT")
    private String templateData;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
