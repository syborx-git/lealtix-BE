package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insumo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** Categorías a las que pertenece el insumo (o la bebida si esBebida=true) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "insumo_category",
            joinColumns = @JoinColumn(name = "insumo_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<TenantMenuCategory> categories = new ArrayList<>();

    @Column(length = 100, nullable = false)
    private String nombre;

    /** Unidad de medida: gramos, mililitros, pieza */
    @Column(length = 20)
    private String unidad;

    /** true = es una bebida (vendible en el POS Comandix); false = insumo de receta */
    @Builder.Default
    private boolean esBebida = false;

    /** Precio de venta al público (solo para bebidas) */
    private BigDecimal precioVenta;

    /** Id del producto de menú (tenant_menu_product) enlazado, solo para bebidas vendibles */
    private Long productoId;

    /** Stock actual del insumo */
    @Builder.Default
    private Double stock = 0.0;

    /** Stock mínimo para alertas */
    @Builder.Default
    private Double stockMinimo = 0.0;

    @Builder.Default
    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
