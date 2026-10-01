package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductAdditional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAdditionalRepository extends JpaRepository<ProductAdditional, Long> {
    List<ProductAdditional> findByDishId(Long dishId);
    Optional<ProductAdditional> findByDishIdAndInsumoId(Long dishId, Long insumoId);
    void deleteByDishId(Long dishId);

    /**
     * Carga en UNA sola consulta todos los adicionales de varios platillos,
     * trayendo el insumo de forma anticipada (join fetch) para evitar N+1.
     */
    @Query("select a from ProductAdditional a join fetch a.insumo where a.dish.id in :dishIds")
    List<ProductAdditional> findByDishIdInWithInsumo(@Param("dishIds") Collection<Long> dishIds);
}
