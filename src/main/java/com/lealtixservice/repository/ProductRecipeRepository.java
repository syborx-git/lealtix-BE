package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
    List<ProductRecipe> findByDishId(Long dishId);
    Optional<ProductRecipe> findByDishIdAndInsumoId(Long dishId, Long insumoId);
    void deleteByDishId(Long dishId);

    @Query("SELECT r FROM ProductRecipe r JOIN FETCH r.insumo WHERE r.dish.id IN :dishIds")
    List<ProductRecipe> findByDishIdInWithInsumo(@Param("dishIds") List<Long> dishIds);

    @Query("SELECT r FROM ProductRecipe r JOIN FETCH r.insumo WHERE r.dish.category.tenant.id = :tenantId")
    List<ProductRecipe> findAllByTenantIdWithInsumo(@Param("tenantId") Long tenantId);
}
