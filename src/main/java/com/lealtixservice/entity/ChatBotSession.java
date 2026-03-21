package com.lealtixservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad que representa una sesión de conversación del ChatBot (Mesero Virtual).
 * Cada sesión está asociada a un tenant y opcionalmente a un cliente identificado.
 */
@Entity
@Table(name = "chatbot_session", indexes = {
        @Index(name = "idx_chatbot_session_tenant", columnList = "tenant_id"),
        @Index(name = "idx_chatbot_session_customer", columnList = "customer_id"),
        @Index(name = "idx_chatbot_session_status", columnList = "status"),
        @Index(name = "idx_chatbot_session_started_at", columnList = "started_at"),
        @Index(name = "idx_chatbot_session_last_interaction", columnList = "last_interaction_at")
})
@Getter
@Setter
@ToString(exclude = {"tenant", "customer", "messages"})
@EqualsAndHashCode(exclude = {"tenant", "customer", "messages"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    @NotBlank
    private String sessionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private TenantCustomer customer;  // Nullable hasta que se identifique

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    @NotBlank
    @Builder.Default
    private String status = "ACTIVE";  // ACTIVE, COMPLETED, ABANDONED, ERROR

    @Column(name = "context", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String context;  // JSON con el contexto de la conversación (carrito, preferencias, etc.)

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_interaction_at", nullable = false)
    private LocalDateTime lastInteractionAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatBotMessage> messages;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (lastInteractionAt == null) {
            lastInteractionAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastInteractionAt = LocalDateTime.now();
    }

    /**
     * Marca la sesión como completada
     */
    public void complete() {
        this.status = "COMPLETED";
        this.endedAt = LocalDateTime.now();
    }

    /**
     * Marca la sesión como abandonada
     */
    public void abandon() {
        this.status = "ABANDONED";
        this.endedAt = LocalDateTime.now();
    }

    /**
     * Marca la sesión como error
     */
    public void error() {
        this.status = "ERROR";
        this.endedAt = LocalDateTime.now();
    }
}
