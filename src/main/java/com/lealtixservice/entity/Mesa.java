package com.lealtixservice.entity;

import com.lealtixservice.enums.MesaEstado;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mesa", indexes = {
        @Index(name = "idx_mesa_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mesa_tenant_estado", columnList = "tenant_id,estado")
})
public class Mesa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(length = 100, nullable = false)
    private String nombre;

    private Integer numero;

    @Builder.Default
    private Integer capacidad = 4;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MesaEstado estado = MesaEstado.LIBRE;

    /** TenantUser (rol MESERO) asignado a la mesa, nullable mientras la mesa esté libre */
    @Column(name = "mesero_user_id")
    private Long meseroUserId;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = MesaEstado.LIBRE;
        }
        if (capacidad == null) {
            capacidad = 4;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}