package com.lealtixservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_compra_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    @JsonBackReference
    private OrdenCompra ordenCompra;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(name = "insumo_nombre", length = 150, nullable = false)
    private String insumoNombre;

    /** 'COCINA' | 'BARRA' | 'ALMACEN' */
    @Builder.Default
    @Column(name = "area_destino", length = 30, nullable = false)
    private String areaDestino = "COCINA";

    @Column(length = 30)
    private String unidad;

    @Builder.Default
    @Column(name = "stock_actual", precision = 12, scale = 3)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_minimo", precision = 12, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cantidad_sugerida", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadSugerida = BigDecimal.ZERO;

    @Column(name = "cantidad_comprada", precision = 12, scale = 3)
    private BigDecimal cantidadComprada;

    @Builder.Default
    @Column(name = "costo_unitario_estimado", precision = 12, scale = 2)
    private BigDecimal costoUnitarioEstimado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "costo_unitario_real", precision = 12, scale = 2)
    private BigDecimal costoUnitarioReal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "subtotal_estimado", precision = 12, scale = 2)
    private BigDecimal subtotalEstimado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "subtotal_real", precision = 12, scale = 2)
    private BigDecimal subtotalReal = BigDecimal.ZERO;

    @Builder.Default
    private Boolean surtido = false;
}

