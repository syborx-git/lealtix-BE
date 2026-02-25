package com.lealtixservice.repository;

import com.lealtixservice.entity.ProductCrossSelling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCrossSellingRepository extends JpaRepository<ProductCrossSelling, Long> {
    
    /**
     * Encuentra sugerencias de productos activas para un producto específico y tenant.
     * Incluye validación de tenant_id en ambos productos (principal y sugerido) para garantizar aislamiento SaaS.
     * Ordena por display_order para mantener el orden de prioridad definido.
     */
    @Query("SELECT pcs FROM ProductCrossSelling pcs " +
           "JOIN FETCH pcs.suggestedProduct sp " +
           "JOIN FETCH sp.category " +
           "WHERE pcs.product.id = :productId " +
           "AND pcs.tenant.id = :tenantId " +
           "AND pcs.isActive = true " +
           "AND sp.isActive = true " +
           "ORDER BY pcs.displayOrder ASC")
    List<ProductCrossSelling> findActiveSuggestionsByProductAndTenant(
        @Param("productId") Long productId, 
        @Param("tenantId") Long tenantId
    );
    
    /**
     * Encuentra todas las configuraciones de cross-selling de un tenant (incluidas inactivas).
     * Útil para el panel de administración.
     */
    @Query("SELECT pcs FROM ProductCrossSelling pcs " +
           "JOIN FETCH pcs.product p " +
           "JOIN FETCH pcs.suggestedProduct sp " +
           "WHERE pcs.tenant.id = :tenantId " +
           "ORDER BY p.nombre ASC, pcs.displayOrder ASC")
    List<ProductCrossSelling> findAllByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Encuentra todas las configuraciones de un producto específico.
     */
    @Query("SELECT pcs FROM ProductCrossSelling pcs " +
           "JOIN FETCH pcs.suggestedProduct sp " +
           "WHERE pcs.product.id = :productId " +
           "AND pcs.tenant.id = :tenantId " +
           "ORDER BY pcs.displayOrder ASC")
    List<ProductCrossSelling> findByProductIdAndTenantId(
        @Param("productId") Long productId,
        @Param("tenantId") Long tenantId
    );
    
    /**
     * Verifica si ya existe una configuración entre dos productos.
     */
    boolean existsByProductIdAndSuggestedProductIdAndTenantId(
        Long productId, 
        Long suggestedProductId, 
        Long tenantId
    );
}
