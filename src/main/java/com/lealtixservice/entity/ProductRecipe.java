package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_recipe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platillo que lleva esta receta */
    @ManyToOne
    @JoinColumn(name = "dish_product_id", nullable = false)
    private TenantMenuProduct dish;

    /** Insumo que compone el platillo */
    @ManyToOne
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    /** Cantidad del insumo requerida por platillo */
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    /** true = el cliente puede quitarlo (exclusión) */
    @Builder.Default
    private Boolean modificable = false;
}
