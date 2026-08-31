package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(length = 100, nullable = false)
    private String nombre;

    /** Unidad de medida: gramos, mililitros, pieza */
    @Column(length = 20)
    private String unidad;

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
