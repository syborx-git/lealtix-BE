package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos", indexes = {
        @Index(name = "idx_pagos_turno", columnList = "id_turno"),
        @Index(name = "idx_pagos_comanda", columnList = "id_comanda"),
        @Index(name = "idx_pagos_fecha", columnList = "tenant_id, fecha")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comanda", nullable = false)
    private ClientOrder comanda;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_turno", nullable = false)
    private Turno turno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cajero", nullable = false)
    private TenantUser cajero;

    @Column(name = "metodo_pago", nullable = false, length = 40)
    private String metodoPago;

    @Column(name = "monto_cuenta", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoCuenta;

    @Builder.Default
    @Column(name = "monto_propina", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoPropina = BigDecimal.ZERO;

    @Column(name = "monto_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "APLICADO";

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (montoCuenta == null) {
            montoCuenta = BigDecimal.ZERO;
        }
        if (montoPropina == null) {
            montoPropina = BigDecimal.ZERO;
        }
        montoTotal = montoCuenta.add(montoPropina);
        if (estado == null) {
            estado = "APLICADO";
        }
    }
}
