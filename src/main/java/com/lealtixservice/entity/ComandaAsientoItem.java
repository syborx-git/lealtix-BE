package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vinculación entre un ítem de la comanda y el asiento/persona que lo consume.
 * Un ítem solo puede pertenecer a un único asiento.
 */
@Entity
@Table(name = "comanda_asiento_item", indexes = {
        @Index(name = "idx_comanda_asiento_item_seat", columnList = "seat_id"),
        @Index(name = "idx_comanda_asiento_item_item", columnList = "item_id", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"seat", "item"})
@EqualsAndHashCode(exclude = {"seat", "item"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaAsientoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private ComandaAsiento seat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ClientOrderItem item;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}