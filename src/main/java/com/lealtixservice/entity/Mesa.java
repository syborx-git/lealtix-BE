package com.lealtixservice.entity;

import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.MesaForma;
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

    /** Coordenada X (px) del centro de la mesa sobre el plano del local */
    @Column(name = "posicion_x")
    private Double posicionX;

    /** Coordenada Y (px) del centro de la mesa sobre el plano del local */
    @Column(name = "posicion_y")
    private Double posicionY;

    /** Forma visual de la mesa: redonda | cuadrada | rectangular */
    @Column(name = "forma", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MesaForma forma = MesaForma.cuadrada;

    /** Orientación de la mesa en el plano (grados: 0/90/180/270) */
    @Column(name = "rotacion")
    @Builder.Default
    private Integer rotacion = 0;

    /** UUID temporal que agrupa mesas unidas; NULL cuando la mesa opera sola */
    @Column(name = "id_grupo_temporal", length = 36)
    private String idGrupoTemporal;

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
        if (forma == null) {
            forma = MesaForma.cuadrada;
        }
        if (rotacion == null) {
            rotacion = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}