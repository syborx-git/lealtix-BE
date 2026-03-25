package com.lealtixservice.repository;

import com.lealtixservice.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    
    @Query("SELECT up FROM UserPermission up WHERE up.tenantUser.id = :tenantUserId")
    List<UserPermission> findByTenantUserId(@Param("tenantUserId") Long tenantUserId);
    
    @Query("SELECT up FROM UserPermission up WHERE up.tenantUser.id = :tenantUserId AND up.permission = :permission")
    Optional<UserPermission> findByTenantUserIdAndPermission(
            @Param("tenantUserId") Long tenantUserId,
            @Param("permission") String permission);
    
    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.tenantUser.id = :tenantUserId AND up.permission = :permission")
    boolean existsByTenantUserIdAndPermission(@Param("tenantUserId") Long tenantUserId, @Param("permission") String permission);
    
    @Query("DELETE FROM UserPermission up WHERE up.tenantUser.id = :tenantUserId")
    void deleteByTenantUserId(@Param("tenantUserId") Long tenantUserId);
}
