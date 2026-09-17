package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bebida del catálogo POS.
 * <p>
 * tipoBebida:
 * <ul>
 *   <li>'directa'  -> se vende por pieza; stock físico 1:1 a través de {@code insumoId}</li>
 *   <li>'preparada'-> disponibilidad calculada en vivo desde {@code bebida_receta} vs stock de barra</li>
 * </ul>
 * Todas las bebidas se venden en unidad 'pieza'.
 */
@Entity
@Table(name = "bebida", uniqueConstraints = {
        @UniqueConstraint(name = "uq_bebida_tenant_nombre", columnNames = {"tenant_id", "nombre"})
}, indexes = {
        @Index(name = "idx_bebida_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bebida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(length = 120, nullable = false)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "precio_venta", nullable = false)
    @Builder.Default
    private BigDecimal precioVenta = BigDecimal.ZERO;

    /** 'directa' | 'preparada' */
    @Column(name = "tipo_bebida", length = 20, nullable = false)
    @Builder.Default
    private String tipoBebida = "directa";

    /** Unidad de venta: todas las bebidas usan 'pieza' */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String unidad = "pieza";

    /** Bebidas directas: insumo físico que se descuenta 1:1 en cada venta */
    @Column(name = "insumo_id")
    private Long insumoId;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Double stockMinimo = 0.0;

    @Builder.Default
    private boolean activo = true;

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