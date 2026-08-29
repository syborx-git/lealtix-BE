package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_additional")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAdditional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platillo al que se le permite este adicional */
    @ManyToOne
    @JoinColumn(name = "dish_product_id", nullable = false)
    private TenantMenuProduct dish;

    /** Insumo que se ofrece como adicional */
    @ManyToOne
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    /** Cantidad fija del adicional */
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    /** Precio extra que se cobra al cliente por este adicional */
    @Builder.Default
    private BigDecimal precio = BigDecimal.ZERO;
}
