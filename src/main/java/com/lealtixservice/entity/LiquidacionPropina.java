package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidaciones_propinas", indexes = {
        @Index(name = "idx_liq_turno_mesero", columnList = "id_turno, id_mesero")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionPropina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liquidacion")
    private Long idLiquidacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_turno", nullable = false)
    private Turno turno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mesero", nullable = false)
    private TenantUser mesero;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cajero", nullable = false)
    private TenantUser cajero;

    @Column(name = "monto_bruto", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoBruto;

    @Builder.Default
    @Column(name = "porcentaje_retencion", precision = 5, scale = 2, nullable = false)
    private BigDecimal porcentajeRetencion = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "monto_retencion", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoRetencion = BigDecimal.ZERO;

    @Column(name = "monto_neto_pagado", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoNetoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @PrePersist
    protected void onCreate() {
        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }
        if (porcentajeRetencion == null) {
            porcentajeRetencion = BigDecimal.ZERO;
        }
        if (montoRetencion == null) {
            montoRetencion = BigDecimal.ZERO;
        }
    }
}
