package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "insumo_id")
    private Long insumoId;

    @Column(name = "insumo_nombre", length = 100)
    private String insumoNombre;

    /** Origen del movimiento (por ahora siempre "bodega"). */
    @Builder.Default
    @Column(name = "origen", length = 20)
    private String origen = "bodega";

    /** Destino del movimiento: "cocina" o "barra". */
    @Column(name = "destino", length = 20)
    private String destino;

    /** Cantidad transferida. */
    @Builder.Default
    private Double cantidad = 0.0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}