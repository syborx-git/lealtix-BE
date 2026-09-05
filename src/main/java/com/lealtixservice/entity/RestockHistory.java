package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "restock_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "insumo_id")
    private Long insumoId;

    @Column(name = "insumo_nombre", length = 100)
    private String insumoNombre;

    /** Cantidad reabastecida. */
    @Builder.Default
    private Double cantidad = 0.0;

    /** Costo total invertido en este restock. */
    @Builder.Default
    private Double costoTotal = 0.0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
