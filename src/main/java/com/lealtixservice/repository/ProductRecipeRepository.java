package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.TenantMenuProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
    List<ProductRecipe> findByDishId(Long dishId);
    Optional<ProductRecipe> findByDishIdAndInsumoId(Long dishId, Long insumoId);
    void deleteByDishId(Long dishId);

    /**
     * Carga en UNA sola consulta todas las recetas de varios platillos,
     * trayendo el insumo de forma anticipada (join fetch) para evitar N+1.
     */
    @Query("select r from ProductRecipe r join fetch r.insumo where r.dish.id in :dishIds")
    List<ProductRecipe> findByDishIdInWithInsumo(@Param("dishIds") Collection<Long> dishIds);
}
