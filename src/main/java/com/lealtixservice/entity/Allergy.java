package com.lealtixservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "allergy", indexes = {
        @Index(name = "idx_allergy_name", columnList = "name")
})
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre normalizado de la alergia (minúsculas, sin acentos, singular). Ej: "almendra". */
    @Column(name = "name", length = 150, nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}