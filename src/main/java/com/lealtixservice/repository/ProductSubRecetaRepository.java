package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductSubReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubRecetaRepository extends JpaRepository<ProductSubReceta, Long> {
    List<ProductSubReceta> findByDishId(Long dishId);
    void deleteByDishId(Long dishId);
    boolean existsByDishIdAndSubRecetaId(Long dishId, Long subRecetaId);
}