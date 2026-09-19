package com.lealtixservice.entity;

import com.lealtixservice.enums.ComandaAsientoEstado;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asiento/persona de una comanda. Permite gestionar una cuenta por comensal
 * y posteriormente dividir la cuenta por asiento generando sub-comandas.
 */
@Entity
@Table(name = "comanda_asiento", indexes = {
        @Index(name = "idx_comanda_asiento_order_id", columnList = "order_id"),
        @Index(name = "idx_comanda_asiento_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@ToString(exclude = {"order"})
@EqualsAndHashCode(exclude = {"order"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ClientOrder order;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "alias", length = 80)
    private String alias;

    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ComandaAsientoEstado estado = ComandaAsientoEstado.ABIERTA;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = ComandaAsientoEstado.ABIERTA;
        }
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        if (numero == null) {
            numero = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}