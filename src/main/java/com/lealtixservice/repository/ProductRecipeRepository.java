package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.TenantMenuProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
    List<ProductRecipe> findByDishId(Long dishId);
    void deleteByDishId(Long dishId);
}
