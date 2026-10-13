package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cortes_caja_diarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_corte_tenant_fecha", columnNames = {"tenant_id", "fecha_corte"})
}, indexes = {
        @Index(name = "idx_cortes_diarios_tenant_fecha", columnList = "tenant_id, fecha_corte")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorteCajaDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_corte_diario")
    private Long idCorteDiario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cajero", nullable = false)
    private TenantUser cajero;

    @Column(name = "cajero_nombre", length = 150)
    private String cajeroNombre;

    @Column(name = "cajero_email", length = 150)
    private String cajeroEmail;

    @Column(name = "fecha_corte", nullable = false)
    private LocalDate fechaCorte;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;

    // ============ MONTOS CALCULADOS POR EL SISTEMA (TEÓRICO) ============
    @Builder.Default
    @Column(name = "sistema_efectivo", precision = 12, scale = 2, nullable = false)
    private BigDecimal sistemaEfectivo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sistema_tarjeta", precision = 12, scale = 2, nullable = false)
    private BigDecimal sistemaTarjeta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sistema_transferencia", precision = 12, scale = 2, nullable = false)
    private BigDecimal sistemaTransferencia = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sistema_otros", precision = 12, scale = 2, nullable = false)
    private BigDecimal sistemaOtros = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sistema_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal sistemaTotal = BigDecimal.ZERO;

    // ============ MONTOS REALES CONTADOS / DECLARADOS EN CAJA (FÍSICO) ============
    @Builder.Default
    @Column(name = "real_efectivo", precision = 12, scale = 2, nullable = false)
    private BigDecimal realEfectivo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "real_tarjeta", precision = 12, scale = 2, nullable = false)
    private BigDecimal realTarjeta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "real_transferencia", precision = 12, scale = 2, nullable = false)
    private BigDecimal realTransferencia = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "real_otros", precision = 12, scale = 2, nullable = false)
    private BigDecimal realOtros = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "real_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal realTotal = BigDecimal.ZERO;

    // ============ DIFERENCIAS (REAL - SISTEMA) ============
    @Builder.Default
    @Column(name = "diferencia_efectivo", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferenciaEfectivo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "diferencia_tarjeta", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferenciaTarjeta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "diferencia_transferencia", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferenciaTransferencia = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "diferencia_otros", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferenciaOtros = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "diferencia_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal diferenciaTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "estado_diferencia", length = 20, nullable = false)
    private String estadoDiferencia = "CUADRADO";

    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;

    @Builder.Default
    @Column(name = "total_comandas")
    private Long totalComandas = 0L;

    @Builder.Default
    @Column(name = "total_articulos")
    private Long totalArticulos = 0L;

    @Builder.Default
    @Column(name = "total_propinas", precision = 12, scale = 2)
    private BigDecimal totalPropinas = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (fechaHoraRegistro == null) {
            fechaHoraRegistro = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (fechaCorte == null) {
            fechaCorte = LocalDate.now();
        }
    }
}
