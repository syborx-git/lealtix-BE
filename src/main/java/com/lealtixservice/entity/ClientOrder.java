package com.lealtixservice.entity;

import com.lealtixservice.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_order", indexes = {
        @Index(name = "idx_client_order_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_client_order_customer_id", columnList = "customer_id"),
        @Index(name = "idx_client_order_tenant_customer", columnList = "tenant_id,customer_id"),
        @Index(name = "idx_client_order_tenant_fecha", columnList = "tenant_id,fecha"),
        @Index(name = "idx_client_order_tenant_estado", columnList = "tenant_id,estado"),
        @Index(name = "idx_client_order_fecha", columnList = "fecha"),
        @Index(name = "idx_client_order_coupon_id", columnList = "coupon_id"),
        @Index(name = "idx_client_order_tenant_coupon", columnList = "tenant_id,coupon_id")
})
@Getter
@Setter
@ToString(exclude = {"customer", "tenant", "items"})
@EqualsAndHashCode(exclude = {"customer", "tenant", "items"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private TenantCustomer customer;  // Nullable para soportar "Venta General"

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull
    private Tenant tenant;

    @Column(name = "coupon_id")
    private Long couponId;  // Nullable, referencia opcional al cupón usado

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus estado;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento", precision = 10, scale = 2, nullable = false)
    private BigDecimal descuento;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientOrderItem> items;

    @Column(name = "source", length = 20)
    private String source;  // Origen: 'CHATBOT', 'MANUAL', 'POS', 'WEB', 'MOBILE'

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;  // PENDIENTE -> EN_PREPARACION

    @Column(name = "ready_at")
    private LocalDateTime readyAt;  // EN_PREPARACION -> LISTO

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = OrderStatus.PENDIENTE;
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (descuento == null) {
            descuento = BigDecimal.ZERO;
        }
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (source == null) {
            source = "MANUAL";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
