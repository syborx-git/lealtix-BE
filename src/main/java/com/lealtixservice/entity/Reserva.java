package com.lealtixservice.entity;

import com.lealtixservice.enums.ReservaEstado;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_tenant", columnList = "tenant_id"),
        @Index(name = "idx_reserva_tenant_fecha", columnList = "tenant_id,fecha"),
        @Index(name = "idx_reserva_tenant_estado", columnList = "tenant_id,estado")
})
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "cliente_nombre", length = 150, nullable = false)
    private String clienteNombre;

    @Column(length = 50)
    private String telefono;

    /** Fecha y hora de la reservación */
    @Column(nullable = false)
    private LocalDateTime fecha;

    @Builder.Default
    @Column(name = "numero_personas")
    private Integer numeroPersonas = 1;

    /** Mesa asignada (mesa.id), nullable mientras no se asigne */
    @Column(name = "mesa_id")
    private Long mesaId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservaEstado estado = ReservaEstado.PENDIENTE;

    @Column(length = 500)
    private String notas;

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
            estado = ReservaEstado.PENDIENTE;
        }
        if (numeroPersonas == null) {
            numeroPersonas = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}