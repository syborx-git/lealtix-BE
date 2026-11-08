package com.lealtixservice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_compra")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(length = 50, nullable = false)
    private String folio;

    @Column(length = 150)
    private String titulo;

    /** PENDIENTE, SURTIDA, CANCELADA */
    @Builder.Default
    @Column(length = 30, nullable = false)
    private String estado = "PENDIENTE";

    @Builder.Default
    @Column(name = "total_estimado", precision = 12, scale = 2)
    private BigDecimal totalEstimado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_real", precision = 12, scale = 2)
    private BigDecimal totalReal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<OrdenCompraItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime surtidaAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = "PENDIENTE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

