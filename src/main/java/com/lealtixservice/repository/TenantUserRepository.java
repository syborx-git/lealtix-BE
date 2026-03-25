package com.lealtixservice.repository;

import com.lealtixservice.entity.TenantUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {
    
    @Query("SELECT tu FROM TenantUser tu WHERE tu.id = :id AND tu.tenant.id = :tenantId AND tu.activo = true")
    Optional<TenantUser> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
    @Query("SELECT tu FROM TenantUser tu WHERE tu.email = :email AND tu.tenant.id = :tenantId AND tu.activo = true")
    Optional<TenantUser> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);
    
    @Query("SELECT tu FROM TenantUser tu WHERE tu.tenant.id = :tenantId AND tu.activo = true " +
           "AND (LOWER(tu.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(tu.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TenantUser> findByTenantIdAndSearch(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT tu FROM TenantUser tu WHERE tu.tenant.id = :tenantId AND tu.activo = true")
    Page<TenantUser> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
    
    @Query("SELECT CASE WHEN COUNT(tu) > 0 THEN true ELSE false END FROM TenantUser tu WHERE tu.email = :email AND tu.tenant.id = :tenantId AND tu.activo = true")
    boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);
    
    @Query("SELECT tu FROM TenantUser tu WHERE tu.email = :email AND tu.activo = true")
    Optional<TenantUser> findByEmail(@Param("email") String email);
}
