package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merma", indexes = {
        @Index(name = "idx_merma_tenant_fecha", columnList = "tenant_id,fecha"),
        @Index(name = "idx_merma_order_id", columnList = "order_id"),
        @Index(name = "idx_merma_registro_id", columnList = "registro_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** Ticket/folio de la comanda origen, ej. #A1B2C3D4 (null en mermas administrativas) */
    @Column(name = "ticket", length = 20, nullable = true)
    private String ticket;

    /** Id de la comanda origen (sin FK para conservar la auditoría aunque se depuren registros) */
    @Column(name = "order_id")
    private UUID orderId;

    /** Agrupa todos los registros capturados en un mismo evento de merma */
    @Column(name = "registro_id", nullable = false)
    private UUID registroId;

    /** Tipificación de la salida No-Venta: OPERATIVA, ROTURA, CADUCIDAD, TRANSFORMACION... */
    @Column(name = "tipo_merma", length = 30)
    @Builder.Default
    private String tipoMerma = "OPERATIVA";

    /** Categoría del registro: COMANDADA (por comanda) o ADMINISTRATIVA (almacén) */
    @Column(name = "categoria_merma", length = 20)
    @Builder.Default
    private String categoriaMerma = "COMANDADA";

    /** Almacén de origen en mermas administrativas: BODEGA | COCINA | BARRA */
    @Column(name = "origen", length = 20)
    private String origen;

    /** Motivo de la merma (texto libre): caducidad, accidente, sobrante... */
    @Column(name = "motivo", length = 255)
    private String motivo;

    /** Usuario que registró la merma (trazabilidad) */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 120)
    private String usuarioNombre;

    @Column(name = "insumo_id")
    private Long insumoId;

    @Column(name = "insumo_nombre", length = 120)
    private String insumoNombre;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "producto_nombre", length = 120)
    private String productoNombre;

    @Column(name = "cantidad", nullable = false)
    private Double cantidad;

    @Column(name = "unidad", length = 20)
    private String unidad;

    /** Costo unitario estimado del insumo (promedio de restocks) */
    @Builder.Default
    private Double costoUnitario = 0.0;

    /** Pérdida económica = cantidad * costoUnitario */
    @Builder.Default
    private Double costoTotal = 0.0;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}