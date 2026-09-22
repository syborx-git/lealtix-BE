package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductAdditional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAdditionalRepository extends JpaRepository<ProductAdditional, Long> {
    List<ProductAdditional> findByDishId(Long dishId);
    Optional<ProductAdditional> findByDishIdAndInsumoId(Long dishId, Long insumoId);
    void deleteByDishId(Long dishId);
}
