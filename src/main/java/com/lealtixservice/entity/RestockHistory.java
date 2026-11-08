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

    /** ID de la orden de compra que origino este restock (opcional). */
    @Column(name = "orden_compra_id")
    private Long ordenCompraId;

    /** Area a la que se ingreso el stock: 'COCINA', 'BARRA', 'ALMACEN'. */
    @Column(name = "area_destino", length = 30)
    private String areaDestino;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
