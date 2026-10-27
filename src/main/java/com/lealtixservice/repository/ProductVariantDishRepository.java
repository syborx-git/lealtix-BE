package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductVariantDish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductVariantDishRepository extends JpaRepository<ProductVariantDish, Long> {
    List<ProductVariantDish> findByDishId(Long dishId);
    List<ProductVariantDish> findByDishIdIn(List<Long> dishIds);
    void deleteByDishId(Long dishId);
    boolean existsByDishIdAndVariantDishId(Long dishId, Long variantDishId);

    /**
     * Carga en una sola consulta las variantes de platillos asignadas a un platillo padre
     * con join fetch para evitar N+1.
     */
    @Query("select v from ProductVariantDish v join fetch v.variantDish where v.dish.id = :dishId")
    List<ProductVariantDish> findByDishIdWithVariantDish(@Param("dishId") Long dishId);

    /**
     * Carga en una sola consulta las variantes de platillos para múltiples productos padre
     * con join fetch para optimizar las consultas del catálogo.
     */
    @Query("select v from ProductVariantDish v join fetch v.variantDish where v.dish.id in :dishIds")
    List<ProductVariantDish> findByDishIdInWithVariantDish(@Param("dishIds") Collection<Long> dishIds);
}
