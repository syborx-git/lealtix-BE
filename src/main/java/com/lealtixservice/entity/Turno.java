package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos", indexes = {
        @Index(name = "idx_turnos_tenant_fecha", columnList = "tenant_id, fecha_apertura")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Long idTurno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cajero", nullable = false)
    private TenantUser cajero;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Builder.Default
    @Column(name = "fondo_inicial", precision = 10, scale = 2, nullable = false)
    private BigDecimal fondoInicial = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_ingresos", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_propinas", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPropinas = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_efectivo_declarado", precision = 10, scale = 2)
    private BigDecimal totalEfectivoDeclarado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "diferencia_caja", precision = 10, scale = 2)
    private BigDecimal diferenciaCaja = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (fondoInicial == null) {
            fondoInicial = BigDecimal.ZERO;
        }
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }
        if (totalPropinas == null) {
            totalPropinas = BigDecimal.ZERO;
        }
        if (estado == null) {
            estado = "ABIERTO";
        }
    }
}
