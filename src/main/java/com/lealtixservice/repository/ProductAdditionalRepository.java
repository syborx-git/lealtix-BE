package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductAdditional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAdditionalRepository extends JpaRepository<ProductAdditional, Long> {
    List<ProductAdditional> findByDishId(Long dishId);
    Optional<ProductAdditional> findByDishIdAndInsumoId(Long dishId, Long insumoId);
    void deleteByDishId(Long dishId);

    @Query("SELECT a FROM ProductAdditional a JOIN FETCH a.insumo WHERE a.dish.id IN :dishIds")
    List<ProductAdditional> findByDishIdInWithInsumo(@Param("dishIds") List<Long> dishIds);

    @Query("SELECT a FROM ProductAdditional a JOIN FETCH a.insumo WHERE a.dish.category.tenant.id = :tenantId")
    List<ProductAdditional> findAllByTenantIdWithInsumo(@Param("tenantId") Long tenantId);
}
