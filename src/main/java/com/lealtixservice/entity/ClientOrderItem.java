package com.lealtixservice.entity;

import com.lealtixservice.util.JsonLongListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_order_item", indexes = {
        @Index(name = "idx_client_order_item_order_id", columnList = "order_id"),
        @Index(name = "idx_client_order_item_product_id", columnList = "product_id"),
        @Index(name = "idx_client_order_item_created_at", columnList = "created_at")
})
@Getter
@Setter
@ToString(exclude = {"order", "product"})
@EqualsAndHashCode(exclude = {"order", "product"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull
    private ClientOrder order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull
    private TenantMenuProduct product;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;

    @Convert(converter = JsonLongListConverter.class)
    @Column(name = "excluded_ingredient_ids", columnDefinition = "TEXT")
    private List<Long> excludedIngredientIds;

    @Convert(converter = JsonLongListConverter.class)
    @Column(name = "additional_ingredient_ids", columnDefinition = "TEXT")
    private List<Long> additionalIngredientIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
