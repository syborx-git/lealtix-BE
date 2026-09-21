package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sub_receta",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_sub_receta",
                columnNames = {"dish_product_id", "sub_receta_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platillo o bebida al que se asigna la sub-receta */
    @ManyToOne
    @JoinColumn(name = "dish_product_id", nullable = false)
    private TenantMenuProduct dish;

    /** Sub-receta (producto esSubReceta=true) cuyo insumos se suman al platillo */
    @ManyToOne
    @JoinColumn(name = "sub_receta_id", nullable = false)
    private TenantMenuProduct subReceta;
}