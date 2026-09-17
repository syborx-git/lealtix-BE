package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Solicitud de stock (Restock Request) enviada por un sub-almacén
 * (Cocina o Barra) hacia la Bodega central.
 */
@Entity
@Table(name = "stock_request", indexes = {
        @Index(name = "idx_stock_request_tenant_estado", columnList = "tenant_id,estado"),
        @Index(name = "idx_stock_request_area", columnList = "area")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** Id del insumo solicitado (opcional; se guarda también el nombre) */
    @Column(name = "insumo_id")
    private Long insumoId;

    @Column(name = "insumo_nombre", length = 120)
    private String insumoNombre;

    /** Área que solicita el insumo: COCINA | BARRA */
    @Column(name = "area", length = 20, nullable = false)
    private String area;

    @Column(name = "cantidad", nullable = false)
    private Double cantidad;

    /** Prioridad de la solicitud: ALTA | MEDIA | BAJA */
    @Column(name = "prioridad", length = 20, nullable = false)
    @Builder.Default
    private String prioridad = "MEDIA";

    /** Estado de la solicitud: PENDIENTE | SURTIDO | CANCELADA */
    @Column(name = "estado", length = 20, nullable = false)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}