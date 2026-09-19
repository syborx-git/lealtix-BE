package com.lealtixservice.entity;

import com.lealtixservice.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sub-comanda de pago por asiento (división de cuenta).
 * Representa el cobro parcial asociado a un asiento de la comanda original,
 * con un folio derivado del folio original (ej: 12345-A).
 */
@Entity
@Table(name = "comanda_pago", indexes = {
        @Index(name = "idx_comanda_pago_order_id", columnList = "order_id"),
        @Index(name = "idx_comanda_pago_seat_id", columnList = "seat_id"),
        @Index(name = "idx_comanda_pago_folio", columnList = "folio")
})
@Getter
@Setter
@ToString(exclude = {"order", "seat", "paidBy"})
@EqualsAndHashCode(exclude = {"order", "seat", "paidBy"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ClientOrder order;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private ComandaAsiento seat;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "folio_original", length = 50)
    private String folioOriginal;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "paid_method", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paidMethod;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @ManyToOne
    @JoinColumn(name = "paid_by")
    private AppUser paidBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estado == null) {
            estado = "PAGADA";
        }
        if (total == null) {
            total = BigDecimal.ZERO;
        }
    }
}