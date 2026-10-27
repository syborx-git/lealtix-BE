package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variant_dish")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platillo o bebida principal al que pertenece esta variante */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_product_id", nullable = false)
    private TenantMenuProduct dish;

    /** Platillo o bebida que actúa como variante */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_dish_id", nullable = false)
    private TenantMenuProduct variantDish;

    /** Precio específico de la variante (si difiere del precio base del platillo variante) */
    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    /** Cantidad por porción o servicio */
    @Builder.Default
    @Column(name = "cantidad")
    private Double cantidad = 1.0;

    /** Unidad de medida (pieza, botella, vaso, copa, etc.) */
    @Column(length = 20)
    @Builder.Default
    private String unidad = "pieza";

    /** Grupo o sección (ej. Presentación, Marca, Sabor) */
    @Column(length = 100)
    private String grupo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
