package com.lealtixservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidad que representa un mensaje individual en una conversación del ChatBot.
 */
@Entity
@Table(name = "chatbot_message", indexes = {
        @Index(name = "idx_chatbot_message_session", columnList = "session_id"),
        @Index(name = "idx_chatbot_message_timestamp", columnList = "timestamp"),
        @Index(name = "idx_chatbot_message_type", columnList = "message_type")
})
@Getter
@Setter
@ToString(exclude = {"session"})
@EqualsAndHashCode(exclude = {"session"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @NotNull
    private ChatBotSession session;

    @Column(name = "message_type", nullable = false, length = 20)
    @NotBlank
    private String messageType;  // TEXT, PRODUCT_SUGGESTION, COUPON_VALIDATION, ORDER_CONFIRMATION, ERROR

    @Column(name = "sender", nullable = false, length = 20)
    @NotBlank
    private String sender;  // USER, BOT, SYSTEM

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String content;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;  // JSON con información adicional (productId, couponCode, etc.)

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
