package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenant_menu_product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMenuProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TenantMenuCategory category;

    /** Todas las categorías a las que pertenece el producto (principal + extras) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tenant_menu_product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<TenantMenuCategory> categories = new ArrayList<>();

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(length = 255)
    private String imgUrl;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Stock actual del producto (inventario) */
    @Builder.Default
    private Double stock = 0.0;

    /** Stock mínimo para alertas */
    @Builder.Default
    private Double stockMinimo = 0.0;

    /** Unidad de medida: gramos, mililitros, pieza */
    @Column(length = 20)
    private String unidad;

    /** true = Platillo (su stock se calcula de sus ingredientes), false = Insumo */
    @Builder.Default
    private Boolean ventaIndividual = false;

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


