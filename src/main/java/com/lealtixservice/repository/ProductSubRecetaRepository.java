package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductSubReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductSubRecetaRepository extends JpaRepository<ProductSubReceta, Long> {
    List<ProductSubReceta> findByDishId(Long dishId);
    void deleteByDishId(Long dishId);
    boolean existsByDishIdAndSubRecetaId(Long dishId, Long subRecetaId);

    /**
     * Carga en UNA sola consulta las sub-recetas asignadas a varios platillos,
     * trayendo la sub-receta de forma anticipada (join fetch) para evitar N+1.
     */
    @Query("select s from ProductSubReceta s join fetch s.subReceta where s.dish.id in :dishIds")
    List<ProductSubReceta> findByDishIdInWithSubReceta(@Param("dishIds") Collection<Long> dishIds);
}
