package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Línea de receta de una bebida 'preparada': cantidad de un insumo por 1 pieza.
 * Ej. Margarita = 50 ml tequila + 20 ml triple sec + 30 ml jugo de limón.
 */
@Entity
@Table(name = "bebida_receta", uniqueConstraints = {
        @UniqueConstraint(name = "uq_bebida_receta", columnNames = {"bebida_id", "insumo_id"})
}, indexes = {
        @Index(name = "idx_bebida_receta_bebida", columnList = "bebida_id"),
        @Index(name = "idx_bebida_receta_insumo", columnList = "insumo_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BebidaReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bebida_id", nullable = false)
    private Long bebidaId;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    /** Cantidad requerida del insumo para preparar 1 pieza de la bebida */
    @Column(nullable = false)
    @Builder.Default
    private Double cantidad = 1.0;

    @Builder.Default
    private boolean modificable = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) this.createdAt = LocalDateTime.now();
    }
}